package cz.tacr.elza.bulkaction;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import javax.transaction.Transactional;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.bulkaction.factory.BulkActionWorkerFactory;
import cz.tacr.elza.bulkaction.generator.BulkAction;
import cz.tacr.elza.domain.ArrBulkActionNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.LevelTreeCacheService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import cz.tacr.elza.bulkaction.factory.BulkActionFactory;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Serviska pro obsluhu hromadných akcí.
 *
 * @author Martin Šlapa
 * @since 11.11.2015
 */
@Service
public class BulkActionService implements InitializingBean, ListenableFutureCallback<BulkActionWorker> {


    /**
     * Počet hromadných akcí v listu MAX_BULK_ACTIONS_LIST.
     */
    public static final int MAX_BULK_ACTIONS_LIST = 100;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private BulkActionFactory bulkActionFactory;

    @Autowired
    private BulkActionRunRepository bulkActionRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    @Autowired
    private BulkActionWorkerFactory workerFactory;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;
    
    @Autowired
    private NodeRepository nodeRepository;

    /**
     * Seznam běžících úloh instancí hromadných akcí.
     *
     * FA_ID -> WORKER
     */
    private HashMap<Integer, BulkActionWorker> runningWorkers = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        bulkActionConfigManager.load();
    }

    /**
     * Testuje, zda-li může být úloha spuštěna/naplánována.
     *
     * @param bulkActionRun úloha, podle které se testuje možné spuštění
     * @return true - pokud se může spustit
     */
    private boolean canRun(final ArrBulkActionRun bulkActionRun) {
        return !runningWorkers.containsKey(bulkActionRun.getFundVersionId());
    }

    /**
     * Smazání nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public void delete(final BulkActionConfig bulkActionConfig) {
        bulkActionConfigManager.delete(bulkActionConfig);
    }

    @Override
    public void onFailure(final Throwable ex) {
        // nenastane, protože ve workeru je catch na Exception
        logger.error("Worker nedoběhl správně", ex);
    }

    @Override
    public void onSuccess(final BulkActionWorker result) {
        runningWorkers.remove(result.getVersionId());
        runNextWorker();
    }

    /**
     * Uložení hromadné akce z klienta
     *
     * @param userId         identfikátor uživatele, který spustil hromadnou akci
     * @param bulkActionCode Kod hromadné akce
     * @param fundVersionId  identifikátor verze archivní pomůcky - je také vstupním uzlem
     * @return objekt hromadné akce
     */
    public ArrBulkActionRun queue(final Integer userId, final String bulkActionCode, final Integer fundVersionId) {
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
        return queue(userId, bulkActionCode, fundVersionId, Collections.singletonList(version.getRootNode().getNodeId()));
    }

    /**
     * Uložení hromadné akce z klienta
     *
     * @param userId         identfikátor uživatele, který spustil hromadnou akci
     * @param bulkActionCode Kod hromadné akce
     * @param fundVersionId  identifikátor verze archivní pomůcky
     * @param inputNodeIds   seznam vstupních uzlů (podstromů AS)
     * @return objekt hromadné akce
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    public ArrBulkActionRun queue(final Integer userId,
                                  final String bulkActionCode,
                                  @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                  final List<Integer> inputNodeIds) {
        Assert.notNull(bulkActionCode);
        Assert.isTrue(StringUtils.isNotBlank(bulkActionCode));
        Assert.notNull(fundVersionId);
        Assert.notEmpty(inputNodeIds);

        ArrBulkActionRun bulkActionRun = new ArrBulkActionRun();

        bulkActionRun.setBulkActionCode(bulkActionCode);
        bulkActionRun.setUserId(userId);
        ArrFundVersion arrFundVersion = new ArrFundVersion();
        arrFundVersion.setFundVersionId(fundVersionId);
        bulkActionRun.setFundVersion(arrFundVersion);

        storeBulkActionRun(bulkActionRun);

        List<ArrBulkActionNode> bulkActionNodes = new ArrayList<>(inputNodeIds.size());
        for (Integer nodeId : inputNodeIds) {
            ArrBulkActionNode bulkActionNode = new ArrBulkActionNode();
            ArrNode arrNode = nodeRepository.findOne(nodeId);
            if (arrNode == null) {
                throw new IllegalArgumentException("Uzel s id " + nodeId + " neexistuje!");
            }
            bulkActionNode.setNode(arrNode);
            bulkActionNode.setBulkActionRun(bulkActionRun);
            bulkActionNodes.add(bulkActionNode);
        }
        storeBulkActionNodes(bulkActionNodes);
        runNextWorker();
        eventPublishBulkAction(bulkActionRun);
        return bulkActionRun;
    }

    /**
     * Spuštění instance hromadné akce.
     *
     * @param bulkActionRun objekt hromadné akce
     * @return objekt hromadné akce
     */
    private ArrBulkActionRun run(final ArrBulkActionRun bulkActionRun) {
        BulkActionWorker bulkActionWorker = workerFactory.getWorker();
        bulkActionWorker.init(bulkActionRun.getBulkActionRunId());
        runningWorkers.put(bulkActionRun.getFundVersionId(), bulkActionWorker);
        runWorker(bulkActionWorker);
        return bulkActionWorker.getBulkActionRun();
    }


    /**
     * Zjistí, zda-li nad verzí AS neběží nějaká hromadná akce.
     *
     * @param version verze AS
     * @return běží nad verzí hromadná akce?
     */
    public boolean isRunning(final ArrFundVersion version) {
        return runningWorkers.containsKey(version.getFundVersionId());
    }

    /**
     * Přenačtení konfigurací hromadných akcí.
     */
    public void reload() {
        try {
            bulkActionConfigManager.load();
        } catch (IOException e) {
            throw new IllegalStateException("Nastal problem při načítání hromadných akcí", e);
        }
    }


    /**
     * Spuštění dalších hromadných akcí, pokud splňují podmínky pro spuštění.
     */
    private void runNextWorker() {
        List<ArrBulkActionRun> waitingActions = bulkActionRepository.findByStateGroupById(State.WAITING);
        waitingActions.forEach(bulkActionRun -> {
            if(canRun(bulkActionRun)) {
                run(bulkActionRun);
            }
        });
    }

    /**
     * Spuštění (předání k naplánování) hromadné akce.
     *
     * @param bulkActionWorker úloha, která se předá k naplánování
     */
    private void runWorker(final BulkActionWorker bulkActionWorker) {
        bulkActionWorker.setStateAndPublish(State.PLANNED);
        logger.info("Hromadná akce naplánována ke spuštění: " + bulkActionWorker);
        ListenableFuture future = taskExecutor.submitListenable(bulkActionWorker);
        future.addCallback(this);
        this.eventNotificationService.forcePublish(
                EventFactory.createStringInVersionEvent(
                        EventType.BULK_ACTION_STATE_CHANGE,
                        bulkActionWorker.getVersionId(),
                        bulkActionWorker.getBulkActionConfig().getCode()
                )
        );
    }

    /**
     * Zvaliduje uzel v nové transakci.
     *
     * @param faLevelId     id uzlu
     * @param fundVersionId id verze
     * @return výsledek validace
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public ArrNodeConformityExt setConformityInfoInNewTransaction(final Integer faLevelId, final Integer fundVersionId) {
        return ruleService.setConformityInfo(faLevelId, fundVersionId);
    }

    /**
     * Přeruší všechny akce pro danou verzi. (všechny naplánované + čekající)
     *
     * @param fundVersionId id verze archivní pomůcky
     */
    public void terminateBulkActions(Integer fundVersionId) {
        bulkActionRepository.findByFundVersionIdAndState(fundVersionId, State.WAITING).forEach(bulkActionRun -> {
            bulkActionRun.setState(State.INTERRUPTED);
            bulkActionRepository.save(bulkActionRun);
        });
        bulkActionRepository.flush();

        if (runningWorkers.containsKey(fundVersionId)) {
            runningWorkers.get(fundVersionId).terminate();
        }
    }

    /**
     * Upravení nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     * @return upravené nastavení hromadné akce
     */
    public BulkActionConfig update(final BulkActionConfig bulkActionConfig) {
        try {
            return bulkActionConfigManager.update(bulkActionConfig);
        } catch (IOException e) {
            throw new IllegalStateException("Problém při aktualizaci hromadné akce", e);
        }
    }

    /// Operace s repositories, getry atd..

    /**
     * Vytvoření nové změny.
     *
     * @param userId the user id
     * @return vytvořená změna
     */
    @Transactional(value = javax.transaction.Transactional.TxType.REQUIRES_NEW)
    public ArrChange createChange(final Integer userId) {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());
        if (userId != null) {
            UsrUser user = new UsrUser();
            user.setUserId(userId);
            change.setUser(user);
        }
        return changeRepository.save(change);
    }

    /**
     * Event publish bulk action.
     *
     * @param bulkActionRun the bulk action run
     */
    public void eventPublishBulkAction(ArrBulkActionRun bulkActionRun) {
        eventNotificationService.forcePublish(
                EventFactory.createStringInVersionEvent(
                        EventType.BULK_ACTION_STATE_CHANGE,
                        bulkActionRun.getFundVersion().getFundVersionId(),
                        bulkActionRun.getBulkActionCode()
                )
        );
    }

    /**
     * Vrací seznam stavů hromadných akcí podle verze archivní pomůcky.
     * <p>
     * - hledá se v seznamu úloh i v databázi
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam stavů hromadných akcí
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    public List<ArrBulkActionRun> getAllArrBulkActionRun(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        return bulkActionRepository.findByFundVersionId(fundVersionId, new PageRequest(0, MAX_BULK_ACTIONS_LIST));
    }

    /**
     * Získání informace o hromadný akce.
     *
     * @param bulkActionRunId   identifikátor hromadné akce
     * @return hromadná akce
     */
    public ArrBulkActionRun getArrBulkActionRun(final Integer bulkActionRunId) {
        Assert.notNull(bulkActionRunId);
        ArrBulkActionRun bulkActionRun = bulkActionRepository.findOne(bulkActionRunId);
        checkAuthBA(bulkActionRun.getFundVersion());
        return bulkActionRun;
    }

    /**
     * Pomocná metoda pro zjištění oprávnění na AS.
     *
     * @param fundVersion verze AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    private void checkAuthBA(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        // pomocná metoda na ověření
    }

    /**
     * Gets bulk action.
     *
     * @param code the code
     * @return the bulk action
     */
    public BulkAction getBulkAction(final String code) {
        return bulkActionFactory.getByCode(code);
    }

    /**
     * Gets bulk action config.
     *
     * @param code the code
     * @return the bulk action config
     */
    public BulkActionConfig getBulkActionConfig(final String code) {
        return bulkActionConfigManager.get(code);
    }

    /**
     * Gets node ids.
     *
     * @param bulkActionRun the bulk action run
     * @return the node ids
     */
    public List<Integer> getBulkActionNodeIds(final ArrBulkActionRun bulkActionRun) {
        return levelTreeCacheService.sortNodesByTreePosition(new HashSet<>(bulkActionNodeRepository.findNodeIdsByBulkActionRun(bulkActionRun)), bulkActionRun.getFundVersion());
    }



    /**
     * Vrací seznam nastavení hromadných akcí podle verze archivní pomůcky.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @param mandatory     true - vrací se pouze seznam povinných, false - vrací se seznam všech
     * @return seznam nastavení hromadných akcí
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    public List<BulkActionConfig> getBulkActions(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                 final boolean mandatory) {
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
        }

        List<BulkActionConfig> bulkActionConfigs = new ArrayList<>();

        for (BulkActionConfig bulkActionConfig : bulkActionConfigManager.getBulkActions()) {
            String ruleCode = (String) bulkActionConfig.getProperty("rule_code");
            if (version.getRuleSet().getCode().equals(ruleCode)) {
                if (!mandatory) {
                    bulkActionConfigs.add(bulkActionConfig);
                }
            }
        }

        return bulkActionConfigs;
    }

    /**
     * Spustí validaci verze AP.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam konfigurací hromadných akcí, které je nutné ještě spustit před uzavřením verze
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    public List<BulkActionConfig> runValidation(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
        }

        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Verze archivní je již uzavřená!");
        }

        List<BulkActionConfig> bulkActionConfigReturnList = new ArrayList<>();

        List<BulkActionConfig> bulkActionConfigMandatoryList = getBulkActions(fundVersionId, true);
        List<ArrBulkActionRun> bulkActions = bulkActionRepository.findByFundVersionId(fundVersionId);

        for (BulkActionConfig bulkActionConfig : bulkActionConfigMandatoryList) {

            boolean isValidate = false;
            for (ArrBulkActionRun bulkAction : bulkActions) {
                if (bulkAction.getBulkActionCode().equals(bulkActionConfig.getCode())) {
                    isValidate = true;
                    break;
                }
            }

            if (!isValidate) {
                bulkActionConfigReturnList.add(bulkActionConfig);
            }
        }

        return bulkActionConfigMandatoryList;
    }

    /**
     * Store bulk action run.
     *
     * @param bulkActionRun the bulk action run
     */
    public void storeBulkActionRun(ArrBulkActionRun bulkActionRun) {
        if (bulkActionRun.getBulkActionRunId() == null) {
            BulkActionConfig bulkActionConfigOrig = bulkActionConfigManager.get(bulkActionRun.getBulkActionCode());

            if (bulkActionConfigOrig == null) {
                throw new IllegalArgumentException("Hromadná akce neexistuje!");
            }

            ArrFundVersion version = fundVersionRepository.findOne(bulkActionRun.getFundVersion().getFundVersionId());

            if (version == null) {
                throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
            }

            if (version.getLockChange() != null) {
                throw new IllegalArgumentException("Verze archivní pomůcky je uzamčená!");
            }

            bulkActionRun.setFundVersion(version);

            String ruleCode = (String) bulkActionConfigOrig.getProperty("rule_code");
            if (ruleCode == null || !version.getRuleSet().getCode().equals(ruleCode)) {
                throw new IllegalArgumentException("Nastavení kódu pravidel (rule_code: " + ruleCode
                        + ") hromadné akce neodpovídá verzi archivní pomůcky (rule_code: " + version.getRuleSet().getCode()
                        + ")!");
            }
        }

        bulkActionRepository.save(bulkActionRun);
    }

    /**
     * Uloží uzly hromadné akce
     *
     * @param bulkActionNodes the bulk action nodes
     */
    public void storeBulkActionNodes(List<ArrBulkActionNode> bulkActionNodes) {
        bulkActionNodeRepository.save(bulkActionNodes);
    }
}
