package cz.tacr.elza.bulkaction;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import cz.tacr.elza.bulkaction.generator.CleanDescriptionItemBulkAction;
import cz.tacr.elza.bulkaction.generator.FindingAidValidationBulkAction;
import cz.tacr.elza.bulkaction.generator.SerialNumberBulkAction;
import cz.tacr.elza.bulkaction.generator.UnitIdBulkAction;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFaBulkAction;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.repository.FaBulkActionRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import org.springframework.beans.factory.annotation.Qualifier;


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
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private BulkActionFactory bulkActionFactory;

    @Autowired
    private FaBulkActionRepository faBulkActionRepository;

    /**
     * Seznam registrovaných typů hromadných akcí.
     */
    private List<String> bulkActionTypes;

    /**
     * Seznam úloh instancí hromadných akcí.
     */
    private List<BulkActionWorker> workers = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        bulkActionTypes = new ArrayList<>();
        bulkActionTypes.add(CleanDescriptionItemBulkAction.TYPE);
        bulkActionTypes.add(SerialNumberBulkAction.TYPE);
        bulkActionTypes.add(UnitIdBulkAction.TYPE);
        bulkActionTypes.add(FindingAidValidationBulkAction.TYPE);
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
     * Spuštění instance hromadné akce ve verzi archivní pomůcky.
     *
     * @param bulkActionConfig    nastavení hromadné akce
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     * @return stav instance hromadné akce
     */
    public BulkActionState run(final BulkActionConfig bulkActionConfig, final Integer findingAidVersionId) {
        BulkActionConfig bulkActionConfigOrig = bulkActionConfigManager.get(bulkActionConfig.getCode());

        if (bulkActionConfigOrig == null) {
            throw new IllegalArgumentException("Hromadná akce neexistuje!");
        }

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(findingAidVersionId);

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

        BulkAction bulkAction = bulkActionFactory
                .getByCode((String) bulkActionConfigOrig.getProperty("code_type_bulk_action"));
        BulkActionWorker bulkActionWorker = new BulkActionWorker(bulkAction, bulkActionConfigOrig, findingAidVersionId);
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
        removeOldFaBulkAction(bulkActionWorker);

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
        removeOldFaBulkAction(bulkActionWorker);

        bulkActionWorker.getBulkActionState().setState(State.PLANNED);
        logger.info("Hromadná akce naplánována ke spuštění: " + bulkActionWorker);
        ListenableFuture future = taskExecutor.submitListenable(bulkActionWorker);
        future.addCallback(this);
    }

    /**
     * Odstraní z databáze záznamy o doběhnutí hromadné akce podle verze a kódu.
     *
     * @param bulkActionWorker úloha, podle které se provede úklid
     */
    private void removeOldFaBulkAction(final BulkActionWorker bulkActionWorker) {
        List<ArrFaBulkAction> bulkActions = faBulkActionRepository.findByFaVersionIdAndBulkActionCode(
                bulkActionWorker.getVersionId(),
                bulkActionWorker.getBulkActionConfig().getCode());
        faBulkActionRepository.delete(bulkActions);
    }

    /**
     * Vrací seznam stavů hromadných akcí podle verze archivní pomůcky.
     *
     * - hledá se v seznamu úloh i v databázi
     *
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     * @return seznam stavů hromadných akcí
     */
    public List<BulkActionState> getBulkActionState(final Integer findingAidVersionId) {
        List<BulkActionState> bulkActionStates = new ArrayList<>();
        for (BulkActionWorker worker : workers) {
            if (worker.getVersionId().equals(findingAidVersionId)) {
                bulkActionStates.add(worker.getBulkActionState());
            }
        }
        for (ArrFaBulkAction arrFaBulkAction : faBulkActionRepository.findByFaVersionId(findingAidVersionId)) {
            boolean add = true;
            for (BulkActionWorker worker : workers) {
                if (worker.getVersionId().equals(findingAidVersionId) && worker.getBulkActionConfig().getCode()
                        .equals(arrFaBulkAction.getBulkActionCode())) {
                    add = false;
                    break;
                }
            }
            if (add) {
                BulkActionState state = new BulkActionState();
                state.setState(State.FINISH);
                state.setRunChange(arrFaBulkAction.getChange());
                state.setBulkActionCode(arrFaBulkAction.getBulkActionCode());
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
        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(result.getVersionId());
        if (findingAidVersion == null) {
            logger.warn("Neexistuje verze archivní pomůcky s identifikátorem " + result.getVersionId());
            return;
        }

        ArrFaBulkAction arrFaBulkAction = new ArrFaBulkAction();

        arrFaBulkAction.setBulkActionCode(result.getBulkActionConfig().getCode());
        arrFaBulkAction.setChange(result.getBulkActionState().getRunChange());
        arrFaBulkAction.setFindingAidVersion(findingAidVersion);

        faBulkActionRepository.save(arrFaBulkAction);

        runNextWorker();
    }

    /**
     * Vrací seznam nastavení hromadných akcí podle verze archivní pomůcky.
     *
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     * @param mandatory           true - vrací se pouze seznam povinných, false - vrací se seznam všech
     * @return seznam nastavení hromadných akcí
     */
    public List<BulkActionConfig> getBulkActions(final Integer findingAidVersionId, final boolean mandatory) {
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(findingAidVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
        }

        final String arrangmentTypeCode = version.getArrangementType().getCode();

        List<BulkActionConfig> bulkActionConfigs = new ArrayList<>();

        for (BulkActionConfig bulkActionConfig : bulkActionConfigManager.getBulkActions()) {
            String ruleCode = (String) bulkActionConfig.getProperty("rule_code");
            if (version.getRuleSet().getCode().equals(ruleCode)) {
                String mandatoryArrangementTypeString = (String) bulkActionConfig
                        .getProperty("mandatory_arrangement_type");

                mandatoryArrangementTypeString = mandatoryArrangementTypeString == null ? ""
                                                                                        : mandatoryArrangementTypeString;
                List<String> mandatoryArrangementType = Arrays.asList(mandatoryArrangementTypeString.split("\\|"));

                if (!mandatory) {
                    bulkActionConfigs.add(bulkActionConfig);
                } else if (mandatoryArrangementType.contains(arrangmentTypeCode) && mandatory) {
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
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     * @return seznam konfigurací hromadných akcí, které je nutné ještě spustit před uzavřením verze
     */
    public List<BulkActionConfig> runValidation(final Integer findingAidVersionId) {
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(findingAidVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
        }

        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Verze archivní je již uzavřená!");
        }

        ArrChange lastUserChange = version.getLastChange();

        List<BulkActionConfig> bulkActionConfigReturnList = new ArrayList<>();

        List<BulkActionConfig> bulkActionConfigMandatoryList = getBulkActions(findingAidVersionId, true);
        List<ArrFaBulkAction> faBulkActions = faBulkActionRepository.findByFaVersionId(findingAidVersionId);

        for (BulkActionConfig bulkActionConfig : bulkActionConfigMandatoryList) {

            boolean isValidate = false;
            for (ArrFaBulkAction bulkAction : faBulkActions) {
                if (bulkAction.getBulkActionCode().equals(bulkActionConfig.getCode())) {
                    if (bulkAction.getChange().getChangeId() > lastUserChange.getChangeId()) {
                        isValidate = true;
                    }
                    break;
                }
            }

            if (!isValidate) {
                bulkActionConfigReturnList.add(bulkActionConfig);
            }
        }

        return bulkActionConfigReturnList;
    }

    /**
     * Ukončí běžící akce pro danou verzi.
     *
     * @param findingAidVersionId id verze archivní pomůcky
     */
    public void terminateBulkActions(Integer findingAidVersionId) {
        for (BulkActionWorker worker : workers) {
            if (worker.getVersionId().equals(findingAidVersionId) && worker.getBulkActionState().getState() == State.RUNNING) {
                worker.terminate();
            }
        }
    }
}
