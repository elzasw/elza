package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.common.TaskExecutor;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventChangeOutputItem;
import cz.tacr.elza.service.eventnotification.events.EventIdAndStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.OutputGeneratorWorker;
import cz.tacr.elza.service.output.OutputRequestStatus;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

@Service
public class OutputServiceInternal {

    private final static Logger logger = LoggerFactory.getLogger(OutputServiceInternal.class);

    private final TaskExecutor taskExecutor = new TaskExecutor(2);

    private final PlatformTransactionManager transactionManager;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final IEventNotificationService eventNotificationService;

    private final OutputDefinitionRepository outputDefinitionRepository;

    private final NodeOutputRepository nodeOutputRepository;

    private final OutputItemRepository outputItemRepository;

    private final FundLevelServiceInternal fundLevelServiceInternal;

    private final EntityManager em;

    private final ResourcePathResolver resourcePathResolver;

    private final ItemService itemService;

    private final ArrangementService arrangementService;

    private final StaticDataService staticDataService;

    private final ItemSettingsRepository itemSettingsRepository;

    private final RuleService ruleService;

    private final ActionRepository actionRepository;

    private final BulkActionRunRepository bulkActionRunRepository;

    @Autowired
    public OutputServiceInternal(PlatformTransactionManager transactionManager,
                                 OutputGeneratorFactory outputGeneratorFactory,
                                 IEventNotificationService eventNotificationService,
                                 FundLevelServiceInternal fundLevelServiceInternal,
                                 OutputDefinitionRepository outputDefinitionRepository,
                                 NodeOutputRepository nodeOutputRepository,
                                 OutputItemRepository outputItemRepository,
                                 EntityManager em,
                                 ResourcePathResolver resourcePathResolver,
                                 ItemService itemService,
                                 ArrangementService arrangementService,
                                 StaticDataService staticDataService,
                                 ItemSettingsRepository itemSettingsRepository,
                                 RuleService ruleService,
                                 ActionRepository actionRepository,
                                 BulkActionRunRepository bulkActionRunRepository) {
        this.transactionManager = transactionManager;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.eventNotificationService = eventNotificationService;
        this.fundLevelServiceInternal = fundLevelServiceInternal;
        this.outputDefinitionRepository = outputDefinitionRepository;
        this.nodeOutputRepository = nodeOutputRepository;
        this.outputItemRepository = outputItemRepository;
        this.em = em;
        this.resourcePathResolver = resourcePathResolver;
        this.itemService = itemService;
        this.arrangementService = arrangementService;
        this.staticDataService = staticDataService;
        this.itemSettingsRepository = itemSettingsRepository;
        this.ruleService = ruleService;
        this.actionRepository = actionRepository;
        this.bulkActionRunRepository = bulkActionRunRepository;
    }

    /**
     * Must be called during application startup within transaction. Initialize output queue
     * dispatcher and recovers interrupted outputs.
     */
    @Transactional(TxType.MANDATORY)
    public void init() {
        // output definition in generating state must be recovered to open state
        List<OutputState> states = Arrays.asList(OutputState.GENERATING, OutputState.COMPUTING);
        int affected = outputDefinitionRepository.setStateFromStateWithError(states, OutputState.OPEN,
                "Server shutdown during process");
        if (affected > 0) {
            logger.warn("{} interrupted outputs by server shutdown were recovered to opened state", affected);
        }

        // start queue manager
        taskExecutor.start();
    }

    /**
     * Searches definition.
     *
     * @throws SystemException When definition not found.
     */
    @Transactional
    public ArrOutputDefinition getOutputDefinition(int outputDefinitionId) {
        ArrOutputDefinition definition = outputDefinitionRepository.findOne(outputDefinitionId);
        if (definition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }
        return definition;
    }

    /**
     * Searches definition in generating state and fetches entities for output generator (fund,
     * template and output type).
     *
     * @throws SystemException When definition not found or it has other than generating state.
     */
    @Transactional
    public ArrOutputDefinition getOutputDefinitionForGenerator(int outputDefinitionId) {
        ArrOutputDefinition definition = outputDefinitionRepository.findOneFetchTypeAndTemplateAndFund(outputDefinitionId);
        if (definition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }
        if (definition.getState() != OutputState.GENERATING) {
            throw new SystemException("Processing output must be in GENERATING state", BaseCode.INVALID_STATE)
                    .set("outputDefinitionId", outputDefinitionId).set("outputState", definition.getState());
        }
        return definition;
    }

    /**
     * Searches output nodes for specified definition. Nodes must be valid by specified lock change.
     *
     * @param lockChange null for open version
     */
    @Transactional
    public List<ArrNodeOutput> getOutputNodes(ArrOutputDefinition outputDefinition, ArrChange lockChange) {
        Validate.notNull(outputDefinition);
        if (lockChange == null) {
            return nodeOutputRepository.findByOutputDefinitionAndDeleteChangeIsNull(outputDefinition);
        }
        return nodeOutputRepository.findByOutputDefinitionAndChange(outputDefinition, lockChange);
    }

    /**
     * Searches direct output items and fetches their data. Items must be valid by specified lock
     * change.
     *
     * @param lockChange null for open version
     */
    @Transactional
    public List<ArrOutputItem> getOutputItems(ArrOutputDefinition outputDefinition, ArrChange lockChange) {
        Validate.notNull(outputDefinition);
        if (lockChange == null) {
            return outputItemRepository.findByOutputAndDeleteChangeIsNull(outputDefinition);
        }
        return outputItemRepository.findByOutputAndChange(outputDefinition, lockChange);
    }

    /**
     * Publish event when output item was changed.
     *
     * @param fundVersion
     * @param outputItem related definition is needed (should be fetched)
     */
    @Transactional(TxType.MANDATORY)
    public void publishOutputItemChanged(ArrOutputItem outputItem, int fundVersionId) {
        ArrOutputDefinition definition = outputItem.getOutputDefinition();
        eventNotificationService.publishEvent(new EventChangeOutputItem(EventType.OUTPUT_ITEM_CHANGE, fundVersionId,
                outputItem.getDescItemObjectId(), definition.getOutputDefinitionId(), definition.getVersion()));
    }

    /**
     * Publish event when output state was changed. Current output definition state is used.
     */
    @Transactional(TxType.MANDATORY)
    public void publishOutputStateChanged(ArrOutputDefinition outputDefinition, int fundVersionId) {
        int outputDefinitionId = outputDefinition.getOutputDefinitionId();
        String outputState = outputDefinition.getState().name();
        publishOutputStateChangedInternal(outputDefinitionId, fundVersionId, outputState);
    }

    /**
     * Publish event when output generation failed, it's special "ERROR" state for client.
     */
    @Transactional(TxType.MANDATORY)
    public void publishOutputFailed(ArrOutputDefinition outputDefinition, int fundVersionId) {
        publishOutputStateChangedInternal(outputDefinition.getOutputDefinitionId(), fundVersionId, "ERROR");
    }

    private void publishOutputStateChangedInternal(int outputDefinitionId, int fundVersionId, String outputState) {
        EventIdAndStringInVersion stateChangedEvent = EventFactory.createStringAndIdInVersionEvent(EventType.OUTPUT_STATE_CHANGE,
                fundVersionId, outputDefinitionId, outputState);
        eventNotificationService.publishEvent(stateChangedEvent);
    }

    /**
     * Creates new change for generating output.
     *
     * @param userId can be null for system administrator
     */
    @Transactional(TxType.MANDATORY)
    public ArrChange createGenerateChange(Integer userId) {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());
        change.setType(ArrChange.Type.GENERATE_OUTPUT);
        if (userId != null) {
            change.setUser(em.getReference(UsrUser.class, userId));
        }
        em.persist(change);
        return change;
    }

    /**
     * Add request for output generation.
     *
     * @param outputDefinitionId
     * @param fundVersionId
     */
    @Transactional(TxType.MANDATORY)
    public OutputRequestStatus addRequest(int outputDefinitionId,
                                          ArrFundVersion fundVersion,
                                          boolean checkBulkActions) {
        // find open output definition
        ArrOutputDefinition definition = getOutputDefinition(outputDefinitionId);
        if (definition.getState() != OutputState.OPEN) {
            throw new SystemException("Requested output must be in OPEN state", BaseCode.INVALID_STATE)
                    .set("expectedState", OutputState.OPEN).set("givenState", definition.getState());
        }

        // check recommended bulk action
        if (checkBulkActions) {
            OutputRequestStatus status = checkRecommendedActions(definition, fundVersion);
            if (status != OutputRequestStatus.OK) {
                return status;
            }
        }

        // save generating state only when caller transaction is committed
        definition.setState(OutputState.GENERATING);

        // create worker
        OutputGeneratorWorker worker = new OutputGeneratorWorker(outputDefinitionId, fundVersion.getFundVersionId(), em,
                outputGeneratorFactory, this, resourcePathResolver, fundLevelServiceInternal, transactionManager, arrangementService);

        // delegate security context
        Runnable runnable = Authorization.createRunnableWithCurrentSecurity(worker);

        // register after commit action
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    taskExecutor.addTask(runnable);
                } else {
                    logger.warn("Request for output is cancelled due to rollback of source transaction, outputDefinitionId:{}",
                            worker.getOutputDefinitionId());
                }
            }
        });

        return OutputRequestStatus.OK;
    }

    /**
     * Finds all outputs defined on specified nodes.
     *
     * @param currentStates state filter, null allows all output states
     */
    @Transactional
    public List<ArrOutputDefinition> findOutputsByNodes(ArrFundVersion fundVersion,
                                                        Collection<Integer> nodeIds,
                                                        OutputState... currentStates) {
        return outputDefinitionRepository.findOutputsByNodes(fundVersion, nodeIds, currentStates);
    }

    /**
     * Updates state for all outputs defined on specified nodes.
     *
     * @param newState new state
     * @param currentStates state filter, null allows all output states
     */
    @Transactional
    public void changeOutputsStateByNodes(ArrFundVersion fundVersion,
                                          Collection<Integer> nodeIds,
                                          OutputState newState,
                                          OutputState... currentStates) {

        List<ArrOutputDefinition> outputDefinitions = findOutputsByNodes(fundVersion, nodeIds, currentStates);
        int fundVersionId = fundVersion.getFundVersionId();

        for (ArrOutputDefinition outputDefinition : outputDefinitions) {
            outputDefinition.setState(newState); // saved by commit
            publishOutputStateChanged(outputDefinition, fundVersionId);
        }
    }

    @Transactional(TxType.MANDATORY)
    public OutputItemConnector createItemConnector(ArrFundVersion fundVersion, ArrOutputDefinition outputDefinition) {
        OutputItemConnectorImpl connector = new OutputItemConnectorImpl(fundVersion, outputDefinition, staticDataService, this, itemService);

        Set<Integer> ignoredItemTypeIds = getIgnoredItemTypeIds(outputDefinition);
        connector.setIgnoredItemTypeIds(ignoredItemTypeIds);

        return connector;
    }

    protected Set<Integer> getIgnoredItemTypeIds(ArrOutputDefinition outputDefinition) {
        Set<Integer> ignoredItemTypeIds = new HashSet<>();

        // add all manually calculating items from settings
        List<ArrItemSettings> itemSettingsList = itemSettingsRepository.findByOutputDefinition(outputDefinition);
        for (ArrItemSettings itemSettings : itemSettingsList) {
            if (itemSettings.getBlockActionResult()) {
                ignoredItemTypeIds.add(itemSettings.getItemTypeId());
            }
        }

        // add all impossible types
        List<RulItemTypeExt> outputItemTypes = ruleService.getOutputItemTypes(outputDefinition);
        for (RulItemTypeExt itemType : outputItemTypes) {
            if (itemType.getType().equals(RulItemType.Type.IMPOSSIBLE)) {
                ignoredItemTypeIds.add(itemType.getItemTypeId());
            }
        }

        return ignoredItemTypeIds;
    }

    @Transactional(TxType.MANDATORY)
    public void createOutputItem(ArrOutputItem outputItem, ArrFundVersion fundVersion, ArrChange createChange) {
        Validate.notNull(createChange);

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Output items cannot be deleted in closed fund version.",
                    ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        RuleSystem ruleSystem = this.staticDataService.getData().getRuleSystems()
                .getByRuleSetId(fundVersion.getFundId());
        itemService.checkValidTypeAndSpec(ruleSystem, outputItem);

        int maxPosition = outputItemRepository.findMaxItemPosition(outputItem.getItemType(), outputItem.getOutputDefinition());

        if (outputItem.getPosition() == null || outputItem.getPosition() > maxPosition) {
            outputItem.setPosition(maxPosition + 1);
        } else {
            // find items which must be moved up
            List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(outputItem.getItemType(),
                    outputItem.getOutputDefinition(), outputItem.getPosition() - 1);
            for (ArrOutputItem item : outputItems) {
                itemService.copyItem(item, createChange, item.getPosition() + 1);
            }
        }

        outputItem.setCreateChange(createChange);
        outputItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        em.persist(outputItem.getData());
        em.persist(outputItem);
    }

    /**
     * Deletes all output items by type for specified definition and fund version.
     *
     * @return Count of affected items
     */
    @Transactional(TxType.MANDATORY)
    public int deleteOutputItemsByType(ArrFundVersion fundVersion,
                                       ArrOutputDefinition outputDefinition,
                                       Integer itemTypeId,
                                       ArrChange deleteChange) {
        Validate.notNull(outputDefinition);
        Validate.notNull(itemTypeId);
        Validate.notNull(deleteChange);

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Output items cannot be deleted in closed fund version.",
                    ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(itemTypeId, outputDefinition);

        for (ArrOutputItem item : outputItems) {
            item.setDeleteChange(deleteChange); // saved by commit
            publishOutputItemChanged(item, fundVersion.getFundVersionId());
        }

        return outputItems.size();
    }

    /**
     * Checks if recommended bulk actions are up-to-date for specified output definition.
     *
     * @param definition
     * @param fundVersion
     * @return
     */
    private OutputRequestStatus checkRecommendedActions(ArrOutputDefinition definition, ArrFundVersion fundVersion) {
        // find output node ids
        List<ArrNodeOutput> outputNodes = getOutputNodes(definition, fundVersion.getLockChange());
        List<Integer> outputNodeIds = new ArrayList<>(outputNodes.size());
        outputNodes.forEach(n -> outputNodeIds.add(n.getNodeId()));

        // find recommended actions
        List<RulAction> recommendedActions = actionRepository.findByRecommendedActionOutputType(definition.getOutputType());
        if (recommendedActions.isEmpty()) {
            return OutputRequestStatus.OK;
        }

        // find finished actions
        List<ArrBulkActionRun> finishedAction = bulkActionRunRepository.findBulkActionsByNodes(fundVersion.getFundVersionId(), outputNodeIds, State.FINISHED);
        if (recommendedActions.size() > finishedAction.size()) {
            return OutputRequestStatus.RECOMMENDED_ACTION_NOT_RUN;
        }

        for (RulAction ra : recommendedActions) {
            ArrChange newestChange = null;
            for (ArrBulkActionRun fa : finishedAction) {
                String code = fa.getBulkActionCode();
                // action and actionRun must match by code
                if (ra.getCode().equals(code)) {
                    ArrChange change = fa.getChange();
                    // replace found if newer change
                    if (newestChange == null || change.getChangeDate().isAfter(newestChange.getChangeDate())) {
                        newestChange = change;
                    }
                }
            }
            // test: bulk action not started
            if (newestChange == null) {
                return OutputRequestStatus.RECOMMENDED_ACTION_NOT_RUN;
            }
            // test: newest change is last change
            for (Integer nodeId : outputNodeIds) {
                if (!fundLevelServiceInternal.isLastChange(newestChange, nodeId, false, true)) {
                    return OutputRequestStatus.DETECT_CHANGE;
                }
            }
        }

        return OutputRequestStatus.OK;
    }
}
