package cz.tacr.elza.bulkaction;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrBulkActionNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelServiceInternal;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputItemConnector;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Serviska pro obsluhu hromadných akcí.
 *
 */
@Service
@Configuration
public class BulkActionService implements ListenableFutureCallback<BulkActionWorker> {

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
    private BulkActionRunRepository bulkActionRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    @Autowired
    private FundLevelServiceInternal fundLevelServiceInternal;

    /**
     * Seznam běžících úloh instancí hromadných akcí.
     *
     * FA_ID -> WORKER
     */
    private HashMap<Integer, BulkActionWorker> runningWorkers = new HashMap<>();

    /**
     * Testuje, zda-li může být úloha spuštěna/naplánována.
     *
     * @param bulkActionRun úloha, podle které se testuje možné spuštění
     * @return true - pokud se může spustit
     */
    private boolean canRun(final ArrBulkActionRun bulkActionRun) {
        return !runningWorkers.containsKey(bulkActionRun.getFundVersionId());
    }


    @Override
    public void onFailure(final Throwable ex) {
        // nenastane, protože ve workeru je catch na Exception
        logger.error("Worker nedoběhl správně: ", ex);
        runNextWorker();
    }

    @Override
    public void onSuccess(final BulkActionWorker result) {
        runningWorkers.remove(result.getVersionId());

        ArrBulkActionRun bulkActionRun = result.getBulkActionRun();

        // změna stavu výstupů na open
        outputServiceInternal.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                bulkActionRun.getArrBulkActionNodes().stream()
                        .map(ArrBulkActionNode::getNodeId)
                        .collect(Collectors.toList()),
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
        Assert.notNull(bulkActionCode, "Musí být vyplněn kód hromadné akce");
        Assert.isTrue(StringUtils.isNotBlank(bulkActionCode), "Musí být vyplněn kód hromadné akce");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notEmpty(inputNodeIds, "Musí být vyplněna alespoň jedna JP");

        ArrBulkActionRun bulkActionRun = new ArrBulkActionRun();

        bulkActionRun.setChange(arrangementService.createChange(ArrChange.Type.BULK_ACTION));
        bulkActionRun.setBulkActionCode(bulkActionCode);
        bulkActionRun.setUserId(userId);
        ArrFundVersion arrFundVersion = new ArrFundVersion();

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(fundVersionId);

        RulRuleSet ruleSet = version.getRuleSet();
        List<RulAction> byRulPackage = actionRepository.findByRuleSet(ruleSet);
        if (byRulPackage.stream().noneMatch(i -> i.getCode().equals(bulkActionCode))) {
            throw new BusinessException("Hromadná akce nepatří do stejných pravidel jako pravidla verze AP.", PackageCode.OTHER_PACKAGE)
                    .set("code", bulkActionCode)
                    .set("ruleSet", ruleSet.getCode());
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
                throw new SystemException("Uzel s id " + nodeId + " neexistuje!", BaseCode.ID_NOT_EXIST);
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
		BulkActionWorker bulkActionWorker = new BulkActionWorker(bulkActionRun, this, txManager);
		bulkActionWorker.init();
        runningWorkers.put(bulkActionRun.getFundVersionId(), bulkActionWorker);

        // změna stavu výstupů na počítání
        outputServiceInternal.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                bulkActionRun.getArrBulkActionNodes().stream()
                        .map(ArrBulkActionNode::getNodeId)
                        .collect(Collectors.toList()),
                OutputState.COMPUTING,
                OutputState.OPEN);

		bulkActionWorker.setStateAndPublish(State.PLANNED);
		logger.info("Hromadná akce naplánována ke spuštění: " + bulkActionWorker);
		BulkActionService actionService = this;

		// worker can be starter only after commit
		// TODO: handle correctly when commit fails -> bulkActionWorker should be removed from runningWorkers
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCommit() {
				ListenableFuture<BulkActionWorker> future = taskExecutor.submitListenable(bulkActionWorker);
				future.addCallback(actionService);
			}
		});
		eventPublishBulkAction(bulkActionWorker.getBulkActionRun());

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
     * Spuštění dalších hromadných akcí, pokud splňují podmínky pro spuštění.
     */
    private void runNextWorker() {
        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
			protected void doInTransactionWithoutResult(final TransactionStatus status) {
				List<Integer> waitingActionsId = bulkActionRepository.findIdByStateGroupByFundOrderById(State.WAITING);
                List<ArrBulkActionRun> waitingActions = bulkActionRepository.findAll(waitingActionsId);
                waitingActions.forEach(bulkActionRun -> {
                    if (canRun(bulkActionRun)) {
                        run(bulkActionRun);
                    }
                });
            }
        });
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
    public void terminateBulkActions(final Integer fundVersionId) {
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

    /// Operace s repositories, getry atd..

    /**
     * Event publish bulk action.
     *
     * @param bulkActionRun the bulk action run
     */
    @Transactional(TxType.MANDATORY)
    public void eventPublishBulkAction(final ArrBulkActionRun bulkActionRun) {
        eventNotificationService.publishEvent(
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
        Assert.notNull(bulkActionRunId, "Identifikátor běhu hromadné akce musí být vyplněn");
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
	@Bean
	@Scope("prototype")
    public BulkAction getBulkAction(final String code) {
		// get configuration object
		BulkActionConfig bac = bulkActionConfigManager.get(code);
		return bac.createBulkAction();
		//return bulkActionFactory.getByCode(code);
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

        List<RulAction> ruleActions = actionRepository.findByRuleSet(version.getRuleSet());
        List<BulkActionConfig> configs = new ArrayList<>(ruleActions.size());

        for (RulAction action : ruleActions) {
            BulkActionConfig config = bulkActionConfigManager.get(action.getCode());
            configs.add(config);
        }

        return configs;
    }

    /**
     * Store bulk action run.
     *
     * @param bulkActionRun the bulk action run
     */
    public void storeBulkActionRun(final ArrBulkActionRun bulkActionRun) {
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
            }

        bulkActionRepository.save(bulkActionRun);
    }

    /**
     * Uloží uzly hromadné akce
     *
     * @param bulkActionNodes the bulk action nodes
     */
    public void storeBulkActionNodes(final List<ArrBulkActionNode> bulkActionNodes) {
        bulkActionNodeRepository.save(bulkActionNodes);
    }

    /**
     * Doběhnutí hromadné akce.
     *
     * @param bulkActionRun objekt hromadné akce
     */
    // TODO: implements concurrent strategy like sub-tree exclusive execute of action or output
    @Transactional(TxType.MANDATORY)
    public void onFinished(final ArrBulkActionRun bulkActionRun) {
        // find action nodes for output update
        List<ArrBulkActionNode> arrBulkActionNodes = bulkActionRun.getArrBulkActionNodes();
        List<Integer> nodeIds = arrBulkActionNodes.stream().map(ArrBulkActionNode::getNodeId).collect(Collectors.toList());

        // find all related output definitions
        List<ArrOutputDefinition> outputDefinitions = outputServiceInternal.findOutputsByNodes(bulkActionRun.getFundVersion(), nodeIds, OutputState.OPEN, OutputState.COMPUTING);

        // prepare ArrChange provider applied only if update occurs, change shared between connectors
        Supplier<ArrChange> changeSupplier = new Supplier<ArrChange>() {
            private ArrChange change;

            @Override
            public ArrChange get() {
                if (change == null) {
                    change = arrangementService.createChange(Type.UPDATE_OUTPUT);
                }
                return change;
            }
        };

        // update each output definition
        logger.info("Dispatching result to outputs");
        for (ArrOutputDefinition definition : outputDefinitions) {
            OutputItemConnector connector = outputServiceInternal.createItemConnector(bulkActionRun.getFundVersion(), definition);
            connector.setChangeSupplier(changeSupplier);

            // update definition by each result
            for (ActionResult result : bulkActionRun.getResult().getResults()) {
                result.createOutputItems(connector);
            }

            // update to open state
            definition.setState(OutputState.OPEN); // saved by commit
            outputServiceInternal.publishOutputStateChanged(definition, bulkActionRun.getFundVersionId());
        }
        logger.info("Result dispatched to outputs");
    }

    /**
     * Searches latest finished bulk actions for specified node ids.
     */
    public List<ArrBulkActionRun> findFinishedBulkActionsByNodeIds(ArrFundVersion fundVersion, Collection<Integer> nodeIds) {
        return bulkActionRepository.findBulkActionsByNodes(fundVersion.getFundVersionId(), nodeIds, State.FINISHED);
    }

    /**
     * Searches latest executions of bulk actions for specified node ids.
     */
    public List<ArrBulkActionRun> findBulkActionsByNodeIds(ArrFundVersion fundVersion, Collection<Integer> nodeIds) {
        return bulkActionRepository.findBulkActionsByNodes(fundVersion.getFundVersionId(), nodeIds, null);
    }

    /**
     * Vyhledá hromadnou akci podle kódu.
     *
     * @param code  kód hromadné akce
     * @return hromadná akce
     */
    public RulAction getBulkActionByCode(final String code) {
        String fileName = RulAction.getFileNameFromCode(code);
        return actionRepository.findOneByFilename(fileName);
    }

    /**
     * Vyhledá hromadnou akci podle kódu.
     *
     * @param codes  kód hromadné akce
     * @return hromadná akce
     */
    public List<RulAction> getBulkActionByCodes(final List<String> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return Collections.emptyList();
        }
        List<String> fileNames = codes.stream().map(RulAction::getFileNameFromCode).collect(Collectors.toList());
        return actionRepository.findByFilenameIn(fileNames);
    }

    public List<RulAction> getRecommendedActions(RulOutputType outputType) {
        return actionRepository.findByRecommendedActionOutputType(outputType);
    }
}
