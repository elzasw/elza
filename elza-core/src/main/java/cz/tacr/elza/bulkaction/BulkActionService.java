package cz.tacr.elza.bulkaction;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import cz.tacr.elza.api.vo.BulkActionState.State;
import cz.tacr.elza.bulkaction.factory.BulkActionFactory;
import cz.tacr.elza.bulkaction.generator.BulkAction;
import cz.tacr.elza.bulkaction.generator.FundValidationBulkAction;
import cz.tacr.elza.bulkaction.generator.SerialNumberBulkAction;
import cz.tacr.elza.bulkaction.generator.UnitIdBulkAction;
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
    private NodeRepository nodeRepository;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    /**
     * Seznam registrovaných typů hromadných akcí.
     */
    private List<String> bulkActionTypes;

    /**
     * Seznam úloh instancí hromadných akcí.
     */
    private List<BulkActionWorker> workers = new ArrayList<>();

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ChangeRepository changeRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        bulkActionTypes = new ArrayList<>();
        bulkActionTypes.add(SerialNumberBulkAction.TYPE);
        bulkActionTypes.add(UnitIdBulkAction.TYPE);
        bulkActionTypes.add(FundValidationBulkAction.TYPE);
        bulkActionConfigManager.load();
    }

    /**
     * Vytvoření nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public BulkActionConfig createBulkAction(final BulkActionConfig bulkActionConfig) {
        BulkActionConfig bulkActionConfigExists = bulkActionConfigManager.get(bulkActionConfig.getCode());
        if (bulkActionConfigExists != null) {
            throw new IllegalArgumentException("Hromadná akce již existuje");
        }
        try {
            bulkActionConfigManager.save(bulkActionConfig);
        } catch (IOException e) {
            throw new IllegalStateException("Problém při vytvoření hromadné akce", e);
        }
        bulkActionConfigManager.put(bulkActionConfig);
        return bulkActionConfig;
    }

    /**
     * Vrací seznam registrovaných typů hromadných akcí.
     *
     * @return seznam typů hromadných akcí
     */
    public List<String> getBulkActionTypes() {
        return bulkActionTypes;
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

    /**
     * Smazání nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public void delete(final BulkActionConfig bulkActionConfig) {
        bulkActionConfigManager.delete(bulkActionConfig);
    }

    /**
     * Spuštění instance hromadné akce ve verzi archivního souboru.
     *
     * @param bulkActionConfig  nastavení hromadné akce
     * @param fundVersionId     identifikátor verze archivní pomůcky
     * @return stav instance hromadné akce
     */
    public BulkActionState run(final BulkActionConfig bulkActionConfig,
                               final Integer fundVersionId) {
        BulkActionConfig bulkActionConfigOrig = bulkActionConfigManager.get(bulkActionConfig.getCode());

        if (bulkActionConfigOrig == null) {
            throw new IllegalArgumentException("Hromadná akce neexistuje!");
        }

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní neexistuje!");
        }

        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Verze archivní pomůcky je uzamčená!");
        }

        String ruleCode = (String) bulkActionConfigOrig.getProperty("rule_code");
        if (ruleCode == null || !version.getRuleSet().getCode().equals(ruleCode)) {
            throw new IllegalArgumentException("Nastavení kódu pravidel (rule_code: " + ruleCode
                    + ") hromadné akce neodpovídá verzi archivní pomůcky (rule_code: " + version.getRuleSet().getCode()
                    + ")!");
        }
        List<Integer> inputNodeIds = Arrays.asList(version.getRootNode().getNodeId());

        return run(fundVersionId, bulkActionConfigOrig, inputNodeIds);
    }

    /**
     * Spuštění instance hromadné akce ve verzi archivní pomůcky.
     *
     * @param bulkActionConfig  nastavení hromadné akce
     * @param fundVersionId     identifikátor verze archivní pomůcky
     * @param inputNodeIds      seznam vstupních uzlů (podstromů AS)
     * @return stav instance hromadné akce
     */
    public BulkActionState run(final BulkActionConfig bulkActionConfig,
                               final Integer fundVersionId,
                               final List<Integer> inputNodeIds) {
        BulkActionConfig bulkActionConfigOrig = bulkActionConfigManager.get(bulkActionConfig.getCode());

        if (bulkActionConfigOrig == null) {
            throw new IllegalArgumentException("Hromadná akce neexistuje!");
        }

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní neexistuje!");
        }

        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Verze archivní pomůcky je uzamčená!");
        }

        String ruleCode = (String) bulkActionConfigOrig.getProperty("rule_code");
        if (ruleCode == null || !version.getRuleSet().getCode().equals(ruleCode)) {
            throw new IllegalArgumentException("Nastavení kódu pravidel (rule_code: " + ruleCode
                    + ") hromadné akce neodpovídá verzi archivní pomůcky (rule_code: " + version.getRuleSet().getCode()
                    + ")!");
        }

        return run(fundVersionId, bulkActionConfigOrig, inputNodeIds);
    }

    /**
     * Spuštění instance hromadné akce.
     *
     * @param fundVersionId         id verze archivního souboru
     * @param bulkActionConfig      nastavení hromadné akce
     * @param inputNodeIds          seznam vstupních uzlů (podstromů AS)
     * @return stav instance hromadné akce
     */
    private BulkActionState run(final Integer fundVersionId, final BulkActionConfig bulkActionConfig, final List<Integer> inputNodeIds) {
        BulkAction bulkAction = bulkActionFactory
                .getByCode((String) bulkActionConfig.getProperty("code_type_bulk_action"));
        BulkActionWorker bulkActionWorker = new BulkActionWorker(bulkAction, bulkActionConfig, fundVersionId, inputNodeIds);
        addWorker(bulkActionWorker);
        runNextWorker();
        return bulkActionWorker.getBulkActionState();
    }

    /**
     * Vrací seznam úloh, které čekají na naplánování.
     *
     * @return seznam úloh
     */
    private List<BulkActionWorker> getWaitingWorkers() {
        List<BulkActionWorker> list = new ArrayList<>();
        for (BulkActionWorker worker : workers) {
            if (worker.getBulkActionState().getState().equals(State.WAITING)) {
                list.add(worker);
            }
        }
        return list;
    }

    /**
     * Přidání ulohy do fronty.
     * - před přidáním se kontroluje, že úloha již není ve frontě (čekající, plánovaná, běžící)
     *
     * @param bulkActionWorker úloha
     */
    public void addWorker(final BulkActionWorker bulkActionWorker) {

        for (BulkActionWorker worker : workers) {
            if (worker.getVersionId().equals(bulkActionWorker.getVersionId())
                    && worker.getBulkActionConfig().getCode().equals(bulkActionWorker.getBulkActionConfig().getCode())
                    && !(worker.getBulkActionState().getState().equals(State.ERROR) || worker.getBulkActionState()
                    .getState().equals(State.FINISH))) {
                throw new IllegalStateException(
                        "Nelze přidat hromadnout akci do fronty, jelikož již taková ve frontě je!");
            }
        }

        logger.info("Hromadná akce přidána do fronty: " + bulkActionWorker);

        // odstraní z fronty předchozí doběhnuté/chybné úlohy
        removeOldWorker(bulkActionWorker);

        // odstraní z db záznamy o doběhnutí hromadné akce
        //removeOldFaBulkAction(bulkActionWorker);

        workers.add(bulkActionWorker);

    }

    /**
     * Testuje, zda-li může být úloha spuštěna/naplánována.
     *
     * @param bulkActionWorker úloha, podle které se testuje možné spuštění
     * @return true - pokud se může spustit
     */
    public boolean canRun(final BulkActionWorker bulkActionWorker) {
        for (BulkActionWorker worker : workers) {
            if (!worker.equals(bulkActionWorker)
                    && worker.getVersionId().equals(bulkActionWorker.getVersionId())
                    && (worker.getBulkActionState().getState().equals(State.PLANNED) || worker.getBulkActionState()
                    .getState().equals(State.RUNNING))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Odstraní z fronty úlohy, které.
     *
     * @param bulkActionWorker úloha, podle které se provede úklid
     */
    private void removeOldWorker(final BulkActionWorker bulkActionWorker) {
        List<BulkActionWorker> remove = new ArrayList<>();
        for (BulkActionWorker worker : workers) {
            if (!worker.equals(bulkActionWorker)
                    && worker.getVersionId().equals(bulkActionWorker.getVersionId())
                    && worker.getBulkActionConfig().getCode().equals(bulkActionWorker.getBulkActionConfig().getCode())
                    && (worker.getBulkActionState().getState().equals(State.ERROR) || worker.getBulkActionState()
                    .getState().equals(State.FINISH))
                    ) {
                remove.add(worker);
            }
        }
        for (BulkActionWorker actionWorker : remove) {
            workers.remove(actionWorker);
        }
    }

    /**
     * Spuštění dalších hromadných akcí, pokud splňují podmínky pro spuštění.
     */
    private void runNextWorker() {
        List<BulkActionWorker> waitingWorkers = getWaitingWorkers();
        for (BulkActionWorker waitingWorker : waitingWorkers) {
            if (canRun(waitingWorker)) {
                runWorker(waitingWorker);
            }
        }
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
     * Spuštění (předání k naplánování) hromadné akce.
     *
     * @param bulkActionWorker úloha, která se předá k naplánování
     */
    private void runWorker(final BulkActionWorker bulkActionWorker) {

        // odstraní z fronty předchozí doběhnuté/chybné úlohy
        removeOldWorker(bulkActionWorker);

        // odstraní z db záznamy o doběhnutí hromadné akce
        //removeOldFaBulkAction(bulkActionWorker);

        bulkActionWorker.getBulkActionState().setState(State.PLANNED);
        logger.info("Hromadná akce naplánována ke spuštění: " + bulkActionWorker);
        ListenableFuture future = taskExecutor.submitListenable(bulkActionWorker);
        future.addCallback(this);
        this.eventNotificationService.forcePublish(EventFactory
                .createStringInVersionEvent(EventType.BULK_ACTION_STATE_CHANGE, bulkActionWorker.getVersionId(),
                        bulkActionWorker.getBulkActionConfig().getCode()));
    }

    /**
     * Odstraní z databáze záznamy o doběhnutí hromadné akce podle verze a kódu.
     *
     * @param bulkActionWorker úloha, podle které se provede úklid
     */
    private void removeOldFaBulkAction(final BulkActionWorker bulkActionWorker) {
        List<ArrBulkActionRun> bulkActions = bulkActionRepository.findByFundVersionIdAndBulkActionCode(
                bulkActionWorker.getVersionId(),
                bulkActionWorker.getBulkActionConfig().getCode());
        bulkActionRepository.delete(bulkActions);
    }

    /**
     * Vrací seznam stavů hromadných akcí podle verze archivní pomůcky.
     *
     * - hledá se v seznamu úloh i v databázi
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam stavů hromadných akcí
     */
    public List<BulkActionState> getBulkActionState(final Integer fundVersionId) {
        List<BulkActionState> bulkActionStates = new ArrayList<>();
        for (BulkActionWorker worker : workers) {
            if (worker.getVersionId().equals(fundVersionId)) {
                bulkActionStates.add(worker.getBulkActionState());
            }
        }
        for (ArrBulkActionRun bulkAction : bulkActionRepository.findByFundVersionId(fundVersionId)) {
            boolean add = true;
            for (BulkActionWorker worker : workers) {
                if (worker.getVersionId().equals(fundVersionId) && worker.getBulkActionConfig().getCode()
                        .equals(bulkAction.getBulkActionCode())) {
                    add = false;
                    break;
                }
            }
            if (add) {
                BulkActionState state = new BulkActionState();
                state.setState(State.FINISH);
                state.setRunChange(bulkAction.getChange());
                state.setBulkActionCode(bulkAction.getBulkActionCode());
                bulkActionStates.add(state);
            }
        }
        return bulkActionStates;
    }


    @Override
    public void onFailure(final Throwable ex) {
        // nenastane, protože ve workeru je catch na Exception
        logger.error("Worker nedoběhl správně", ex);
    }

    @Override
    public void onSuccess(final BulkActionWorker result) {
        ArrFundVersion fundVersion = fundVersionRepository.findOne(result.getVersionId());
        if (fundVersion == null) {
            logger.warn("Neexistuje verze archivní pomůcky s identifikátorem " + result.getVersionId());
            return;
        }

        ArrBulkActionRun arrFaBulkAction = new ArrBulkActionRun();

        arrFaBulkAction.setBulkActionCode(result.getBulkActionConfig().getCode());
        arrFaBulkAction.setChange(result.getBulkActionState().getRunChange());
        arrFaBulkAction.setFundVersion(fundVersion);

        bulkActionRepository.save(arrFaBulkAction);

        List<ArrNode> nodes = nodeRepository.findAll(result.getInputNodeIds());

        List<ArrBulkActionNode> bulkActionNodes = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            ArrBulkActionNode bulkActionNode = new ArrBulkActionNode();
            bulkActionNode.setNode(node);
            bulkActionNode.setBulkActionRun(arrFaBulkAction);
            bulkActionNodes.add(bulkActionNode);
        }
        bulkActionNodeRepository.save(bulkActionNodes);

        runNextWorker();
    }

    /**
     * Vrací seznam nastavení hromadných akcí podle verze archivní pomůcky.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @param mandatory           true - vrací se pouze seznam povinných, false - vrací se seznam všech
     * @return seznam nastavení hromadných akcí
     */
    public List<BulkActionConfig> getBulkActions(final Integer fundVersionId, final boolean mandatory) {
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
     * Vrací nastavení hromadní akce podle kódu.
     *
     * @param bulkActionCode kód nastavení hromadné akce
     * @return nastavení hromadné akce
     */
    public BulkActionConfig getBulkAction(final String bulkActionCode) {
        BulkActionConfig bulkActionConfig = bulkActionConfigManager.get(bulkActionCode);
        if (bulkActionConfig == null) {
            throw new IllegalArgumentException("Hromadná akce neexistuje");
        }
        return bulkActionConfig;
    }

    /**
     * Zjišťuje, zda-li některý z vláken nepoužívá změnu
     *
     * @param change změna
     * @return true - některé z vláken používá tuto změnu
     */
    public boolean existsChangeInWorkers(final ArrChange change) {
        for (BulkActionWorker worker : workers) {
            if (change.equals(worker.getBulkActionState().getRunChange())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Spustí validaci verze AP.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam konfigurací hromadných akcí, které je nutné ještě spustit před uzavřením verze
     */
    public List<BulkActionConfig> runValidation(final Integer fundVersionId) {
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
     * Ukončí běžící akce pro danou verzi.
     *
     * @param fundVersionId id verze archivní pomůcky
     */
    public void terminateBulkActions(Integer fundVersionId) {
        for (BulkActionWorker worker : workers) {
            if (worker.getVersionId().equals(fundVersionId) && worker.getBulkActionState().getState() == State.RUNNING) {
                worker.terminate();
            }
        }
    }


    /**
     * Zvaliduje uzel v nové transakci.
     *
     * @param faLevelId   id uzlu
     * @param fundVersionId id verze
     * @return výsledek validace
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public ArrNodeConformityExt setConformityInfoInNewTransaction(final Integer faLevelId, final Integer fundVersionId) {
        return ruleService.setConformityInfo(faLevelId, fundVersionId);
    }

    /**
     * Vytvoření nové změny.
     *
     * @return vytvořená změna
     */
    @Transactional(value = javax.transaction.Transactional.TxType.REQUIRES_NEW)
    public ArrChange createChange() {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());
        return changeRepository.save(change);
    }
}
