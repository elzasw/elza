package cz.tacr.elza.bulkaction;


import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.bulkaction.factory.BulkActionFactory;
import cz.tacr.elza.bulkaction.factory.BulkActionWorkerFactory;
import cz.tacr.elza.bulkaction.generator.BulkAction;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.utils.Yaml;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    @Qualifier("threadPoolTaskExecutorBA")
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

    @Autowired
    private OutputService outputService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private ActionRecommendedRepository actionRecommendedRepository;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

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
        runNextWorker();
    }

    @Override
    public void onSuccess(final BulkActionWorker result) {
        runningWorkers.remove(result.getVersionId());

        ArrBulkActionRun bulkActionRun = result.getBulkActionRun();

        // změna stavu výstupů na open
        outputService.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                bulkActionRun.getArrBulkActionNodes()
                        .stream()
                        .map(ArrBulkActionNode::getNode)
                        .collect(Collectors.toSet()),
                OutputState.OPEN,
                OutputState.COMPUTING);

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

        bulkActionRun.setChange(arrangementService.createChange());
        bulkActionRun.setBulkActionCode(bulkActionCode);
        bulkActionRun.setUserId(userId);
        ArrFundVersion arrFundVersion = new ArrFundVersion();

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(fundVersionId);

        List<RulAction> byRulPackage = actionRepository.findByRulPackage(version.getRuleSet().getPackage());
        String actionFileName = bulkActionCode + bulkActionConfigManager.getExtension();
        if (byRulPackage.stream().noneMatch(i -> i.getFilename().equals(actionFileName))) {
            throw new IllegalStateException("Hromadná akce nepatří do stejného balíčku pravidel jako pravidla verze AP.");
        }

        arrFundVersion.setFundVersionId(fundVersionId);
        bulkActionRun.setFundVersion(arrFundVersion);
        bulkActionRun.setDatePlanned(new Date());
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
        bulkActionRun.setArrBulkActionNodes(bulkActionNodes);
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

        // změna stavu výstupů na počítání
        outputService.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                bulkActionRun.getArrBulkActionNodes()
                        .stream()
                        .map(ArrBulkActionNode::getNode)
                        .collect(Collectors.toSet()),
                OutputState.COMPUTING,
                OutputState.OPEN);

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
        } catch ( IOException e) {
            throw new IllegalStateException("Nastal problem při načítání hromadných akcí", e);
        }
    }


    /**
     * Spuštění dalších hromadných akcí, pokud splňují podmínky pro spuštění.
     */
    private void runNextWorker() {
        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                List<ArrBulkActionRun> waitingActions = bulkActionRepository.findByStateGroupByFundOrderById(State.WAITING);
                waitingActions.forEach(bulkActionRun -> {
                    if (canRun(bulkActionRun)) {
                        run(bulkActionRun);
                    }
                });
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
        eventPublishBulkAction(bulkActionWorker.getBulkActionRun());
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
     * Přeruší hromadnou akci pokud je ve stavu - čeká | plánování | běh
     *
     * @param bulkActionId Id hromadné akce
     */
    public void interruptBulkAction(final int bulkActionId) {
        ArrBulkActionRun bulkActionRun = bulkActionRepository.findOne(bulkActionId);

        if (bulkActionRun == null) {
            throw new IllegalArgumentException("Hromadná akce s ID " + bulkActionId + " nebyla nalezena!");
        }
        State originalState = bulkActionRun.getState();

        if (!originalState.equals(State.WAITING) && !originalState.equals(State.PLANNED) && !originalState.equals(State.RUNNING)) {
            throw new IllegalArgumentException("Nelze přerušit hromadnou akci ve stavu " + originalState + "!");
        }

        boolean needSave = true;

        if (originalState.equals(State.RUNNING)) {
            if (runningWorkers.containsKey(bulkActionRun.getFundVersionId())) {
                BulkActionWorker bulkActionWorker = runningWorkers.get(bulkActionRun.getFundVersionId());
                if (bulkActionWorker.getBulkActionRun().getBulkActionRunId().equals(bulkActionRun.getBulkActionRunId())) {
                    bulkActionWorker.terminate();
                    needSave = false;
                }
            }
        }

        if (needSave) {
            bulkActionRun.setState(State.INTERRUPTED);
            bulkActionRepository.save(bulkActionRun);
            eventPublishBulkAction(bulkActionRun);
            bulkActionRepository.flush();
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
        } catch (IOException | Yaml.YAMLNotInitializedException e) {
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
                EventFactory.createIdInVersionEvent(
                        EventType.BULK_ACTION_STATE_CHANGE,
                        bulkActionRun.getFundVersion(),
                        bulkActionRun.getBulkActionRunId()
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
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
     * Kontroluje ve verzi dokončené hromadné akce, jestli zda-li jsou aktuální.
     * V případě, že je detekovaná změna, provede změnu stavu hromadné akce na neaktuální.
     *
     * @param fundVersionId id verze archivní pomůcky
     */
    public void checkOutdatedActions(final Integer fundVersionId) {
        List<ArrBulkActionRun> bulkActions = bulkActionRepository.findByFundVersionIdAndState(fundVersionId, State.FINISHED);

        for (ArrBulkActionRun bulkAction : bulkActions) {
            if (bulkAction.getState() == State.FINISHED) {
                Set<ArrNode> arrNodes = bulkAction.getArrBulkActionNodes().stream().map(ArrBulkActionNode::getNode).collect(Collectors.toSet());
                HashSet<ArrChange> arrChanges = new HashSet<>(1);
                ArrChange changeBulkAction = bulkAction.getChange();
                arrChanges.add(changeBulkAction);

                Map<ArrChange, Boolean> arrChangeBooleanMap = arrangementService.detectChangeNodes(arrNodes, arrChanges, true, true);
                if (arrChangeBooleanMap.containsKey(changeBulkAction) && arrChangeBooleanMap.get(changeBulkAction)) {
                    bulkAction.setState(State.OUTDATED);
                    bulkActionRepository.save(bulkAction);
                    eventPublishBulkAction(bulkAction);
                }
            }
        }
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
     * @return seznam nastavení hromadných akcí
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<BulkActionConfig> getBulkActions(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
        }

        Map<String, RulAction> byRulPackage = actionRepository.findByRulPackage(version.getRuleSet().getPackage())
                .stream()
                .collect(Collectors.toMap(RulAction::getFilename, p -> p));

        return bulkActionConfigManager.getBulkActions().stream()
                .filter(i -> byRulPackage.containsKey(i.getCode() + bulkActionConfigManager.getExtension()))
                .collect(Collectors.toList());
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

            String ruleCode = (String) bulkActionConfigOrig.getString("rule_code");
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

    /**
     * Doběhnutí hromadné akce.
     *
     * @param bulkActionRun objekt hromadné akce
     */
    public void finished(final ArrBulkActionRun bulkActionRun) {
        Set<ArrNode> nodes = new HashSet<>();
        List<ArrBulkActionNode> arrBulkActionNodes = bulkActionRun.getArrBulkActionNodes();
        nodes.addAll(arrBulkActionNodes.stream().map(ArrBulkActionNode::getNode).collect(Collectors.toSet()));

        try {
            logger.info("Zahájení překlopení výsledku hromadné akce do výstupů");
            ArrChange change = arrangementService.createChange();
            outputService.storeResultBulkAction(bulkActionRun, nodes, change, null);
            logger.info("Překlopení výsledku hromadné akce bylo úspěšně dokončeno");
        } catch (Exception e) {
            logger.error("Nastal problém při překlopení výsledků hromadné akce do výstupů", e);
        }
    }

    /**
     * Vyhledání výstupů podle uzlů.
     *
     * @param fundVersion verze AS
     * @param nodes       seznam uzlů
     * @return seznam hromadných akcí
     */
    public List<ArrBulkActionRun> findBulkActionsByNodes(final ArrFundVersion fundVersion,
                                                         final Set<ArrNode> nodes) {
        return bulkActionRepository.findBulkActionsByNodes(fundVersion, nodes, State.FINISHED);
    }

    /**
     * Vyhledání výstupů podle uzlů.
     *
     * @param fundVersion verze AS
     * @param nodes       seznam uzlů
     * @return seznam hromadných akcí
     */
    public List<ArrBulkActionRun> findBulkActionsByNodes(final ArrFundVersion fundVersion, final Set<ArrNode> nodes, final State... states) {
        return bulkActionRepository.findBulkActionsByNodes(fundVersion, nodes, states);
    }

    /**
     * Vyhledá hromadnou akci podle kódu.
     *
     * @param code  kód hromadné akce
     * @return hromadná akce
     */
    public RulAction getBulkActionByCode(final String code) {
        return actionRepository.findOneByFilename(code + ".yaml");
    }

    /**
     * Vyhledá hromadnou akci podle kódu.
     *
     * @param codes  kód hromadné akce
     * @return hromadná akce
     */
    public List<RulAction> getBulkActionByCodes(final List<String> codes) {
        return actionRepository.findByFilename(codes.stream().map(code -> code + ".yaml").collect(Collectors.toList()));
    }

    public Set<RulAction> getRecommendedActions(RulOutputType outputType) {
        return actionRecommendedRepository.findByOutputType(outputType).stream().map(RulActionRecommended::getAction).collect(Collectors.toSet());
    }
}
