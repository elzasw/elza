package cz.tacr.elza.service;

import static cz.tacr.elza.repository.ExceptionThrow.output;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.common.TaskExecutor;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputRestrictionScope;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputRestrictionScopeRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.OutputTemplateRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.service.ItemService.FundContext;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventChangeOutputItem;
import cz.tacr.elza.service.eventnotification.events.EventIdAndStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.OutputRequestStatus;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

@Service
public class OutputServiceInternal {

    private final static Logger logger = LoggerFactory.getLogger(OutputServiceInternal.class);

    private final TaskExecutor taskExecutor = new TaskExecutor(2);

    private final OutputFileRepository outputFileRepository;

    private final OutputTemplateRepository outputTemplateRepository;

    private final IEventNotificationService eventNotificationService;

    private final OutputRepository outputRepository;

    private final OutputResultRepository outputResultRepository;

    private final NodeOutputRepository nodeOutputRepository;

    private final OutputItemRepository outputItemRepository;

    private final EntityManager em;

    private final ItemService itemService;

    private final ArrangementService arrangementService;

    private final StaticDataService staticDataService;

    private final ItemSettingsRepository itemSettingsRepository;

    private final RuleService ruleService;

    private final ActionRepository actionRepository;

    private final BulkActionRunRepository bulkActionRunRepository;

    private final RevertingChangesService revertingChangesService;

    private final OutputRestrictionScopeRepository outputRestrictionScopeRepository;
    
    private final TemplateRepository templateRepository;

    private final ApStateRepository stateRepository;
    private final StructObjService structObjService;

    @Autowired
    @Lazy
    private AsyncRequestService asyncRequestService;

    private final DmsService dmsService;

    @Autowired
    public OutputServiceInternal(final IEventNotificationService eventNotificationService,
                                 final OutputRepository outputRepository,
                                 final OutputResultRepository outputResultRepository,
                                 final NodeOutputRepository nodeOutputRepository,
                                 final OutputItemRepository outputItemRepository,
                                 final EntityManager em,
                                 final ItemService itemService,
                                 final ArrangementService arrangementService,
                                 final StaticDataService staticDataService,
                                 final ItemSettingsRepository itemSettingsRepository,
                                 final RuleService ruleService,
                                 final ActionRepository actionRepository,
                                 final BulkActionRunRepository bulkActionRunRepository,
                                 final RevertingChangesService revertingChangesService,
                                 final OutputRestrictionScopeRepository outputRestrictionScopeRepository,
                                 final ApStateRepository stateRepository,
                                 final StructObjService structObjService,
                                 final OutputTemplateRepository outputTemplateRepository,
                                 final TemplateRepository templateRepository,
                                 final OutputFileRepository outputFileRepository,
                                 final DmsService dmsService) {
        this.eventNotificationService = eventNotificationService;
        this.outputRepository = outputRepository;
        this.outputResultRepository = outputResultRepository;
        this.nodeOutputRepository = nodeOutputRepository;
        this.outputItemRepository = outputItemRepository;
        this.em = em;
        this.itemService = itemService;
        this.arrangementService = arrangementService;
        this.staticDataService = staticDataService;
        this.itemSettingsRepository = itemSettingsRepository;
        this.ruleService = ruleService;
        this.actionRepository = actionRepository;
        this.bulkActionRunRepository = bulkActionRunRepository;
        this.revertingChangesService = revertingChangesService;
        this.outputRestrictionScopeRepository = outputRestrictionScopeRepository;
        this.stateRepository = stateRepository;
        this.structObjService = structObjService;
        this.outputTemplateRepository = outputTemplateRepository;
        this.templateRepository = templateRepository;
        this.outputFileRepository = outputFileRepository;
        this.dmsService = dmsService;
    }

    /**
     * Must be called during application startup within transaction. Initialize output queue
     * dispatcher and recovers interrupted outputs.
     */
    @Transactional(TxType.MANDATORY)
    public void init() {
        // output in generating state must be recovered to open state
        List<OutputState> states = Arrays.asList(OutputState.GENERATING, OutputState.COMPUTING);
        int affected = outputRepository.updateStateFromStateWithError(states, OutputState.OPEN,
                "Server shutdown during process");
        if (affected > 0) {
            logger.warn("{} interrupted outputs by server shutdown were recovered to opened state", affected);
        }

        // start queue manager
        taskExecutor.start();
    }

    public void stop() {
        taskExecutor.stop();
    }

    /**
     * Searches output.
     *
     * @throws SystemException When output not found.
     */
    @Transactional
    public ArrOutput getOutput(int outputId) {
        return outputRepository.findById(outputId)
                .orElseThrow(output(outputId));
    }

    /**
     * Searches output in generating state and fetches entities for output generator (fund,
     * template and output type).
     *
     * @throws SystemException When output not found or it has other than generating state.
     */
    @Transactional
    public ArrOutput getOutputForGenerator(int outputId) {
        ArrOutput output = outputRepository.findOneFetchTypeAndFund(outputId);
        if (output == null) {
            throw new SystemException("Output not found", BaseCode.ID_NOT_EXIST).set("outputId", outputId);
        }
        if (output.getState() != OutputState.GENERATING) {
            throw new SystemException("Processing output must be in GENERATING state", BaseCode.INVALID_STATE)
                    .set("outputId", outputId).set("outputState", output.getState());
        }
        return output;
    }

    /**
     * Searches output nodes for specified output. Nodes must be valid by specified lock change.
     *
     * @param lockChange null for open version
     */
    @Transactional
    public List<ArrNodeOutput> getOutputNodes(ArrOutput output, ArrChange lockChange) {
        Validate.notNull(output);
        if (lockChange == null) {
            return nodeOutputRepository.findByOutputAndDeleteChangeIsNull(output);
        }
        return nodeOutputRepository.findByOutputAndChange(output, lockChange);
    }

    /**
     * Searches direct output items and fetches their data. Items must be valid by specified lock
     * change.
     *
     * @param change null for open version
     */
    @Transactional
    public List<ArrOutputItem> getOutputItems(ArrOutput output, ArrChange change) {
        Validate.notNull(output);
        if (change == null) {
            return outputItemRepository.findByOutputAndDeleteChangeIsNull(output);
        }
        return outputItemRepository.findByOutputAndChange(output, change);
    }

    /**
     * Publish event when output item was changed.
     *
     * @param outputItem related output is needed (should be fetched)
     */
    @Transactional(TxType.MANDATORY)
    public void publishOutputItemChanged(ArrOutputItem outputItem, int fundVersionId) {
        ArrOutput output = outputItem.getOutput();
        eventNotificationService.publishEvent(new EventChangeOutputItem(EventType.OUTPUT_ITEM_CHANGE, fundVersionId,
                outputItem.getDescItemObjectId(), output.getOutputId(), output.getVersion()));
    }

    /**
     * Publish event when output state was changed. Current output state is used.
     */
    @Transactional(TxType.MANDATORY)
    public void publishOutputStateChanged(ArrOutput output, int fundVersionId) {
        int outputId = output.getOutputId();
        String outputState = output.getState().name();
        publishOutputStateChangedInternal(outputId, fundVersionId, outputState);
    }

    /**
     * Publish event when output generation failed, it's special "ERROR" state for client.
     */
    @Transactional(TxType.MANDATORY)
    public void publishOutputFailed(ArrOutput output, int fundVersionId) {
        publishOutputStateChangedInternal(output.getOutputId(), fundVersionId, "ERROR");
    }

    private void publishOutputStateChangedInternal(int outputId, int fundVersionId, String outputState) {
        EventIdAndStringInVersion stateChangedEvent = EventFactory.createStringAndIdInVersionEvent(EventType.OUTPUT_STATE_CHANGE,
                fundVersionId, outputId, outputState);
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
        change.setChangeDate(OffsetDateTime.now());
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
     * @param userId
     */
    @Transactional(TxType.MANDATORY)
    public OutputRequestStatus addRequest(int outputId,
                                          ArrFundVersion fundVersion,
                                          boolean checkBulkActions,
                                          Integer userId) {
        // find open output
        ArrOutput output = getOutput(outputId);
        if (output.getState() != OutputState.OPEN) {
            throw new SystemException("Requested output must be in OPEN state", BaseCode.INVALID_STATE)
                    .set("expectedState", OutputState.OPEN).set("givenState", output.getState());
        }

        // check recommended bulk action
        if (checkBulkActions) {
            OutputRequestStatus status = checkRecommendedActions(output, fundVersion);
            if (status != OutputRequestStatus.OK) {
                return status;
            }
        }

        // save generating state only when caller transaction is committed
        output.setState(OutputState.GENERATING);

        asyncRequestService.enqueue(fundVersion, output, userId);

        return OutputRequestStatus.OK;
    }

    /**
     * Finds all outputs on specified nodes.
     *
     * @param currentStates state filter, null allows all output states
     */
    @Transactional
    public List<ArrOutput> findOutputsByNodes(ArrFundVersion fundVersion,
                                              Collection<Integer> nodeIds,
                                              OutputState... currentStates) {
        return outputRepository.findOutputsByNodes(fundVersion, nodeIds, currentStates);
    }

    /**
     * Updates state for all outputs on specified nodes.
     *
     * @param newState new state
     * @param currentStates state filter, null allows all output states
     */
    @Transactional
    public void changeOutputsStateByNodes(ArrFundVersion fundVersion,
                                          Collection<Integer> nodeIds,
                                          OutputState newState,
                                          OutputState... currentStates) {

        List<ArrOutput> outputs = findOutputsByNodes(fundVersion, nodeIds, currentStates);
        int fundVersionId = fundVersion.getFundVersionId();

        for (ArrOutput output : outputs) {
            output.setState(newState); // saved by commit
            publishOutputStateChanged(output, fundVersionId);
        }
    }

    @Transactional(TxType.MANDATORY)
    public OutputItemConnector createItemConnector(ArrFundVersion fundVersion, ArrOutput output) {
        OutputItemConnectorImpl connector = new OutputItemConnectorImpl(fundVersion, output, staticDataService, this, itemService, structObjService);

        Set<Integer> ignoredItemTypeIds = getIgnoredItemTypeIds(output);
        connector.setIgnoredItemTypeIds(ignoredItemTypeIds);

        return connector;
    }

    protected Set<Integer> getIgnoredItemTypeIds(ArrOutput output) {
        Set<Integer> ignoredItemTypeIds = new HashSet<>();

        // add all manually calculating items from settings
        List<ArrItemSettings> itemSettingsList = itemSettingsRepository.findByOutput(output);
        for (ArrItemSettings itemSettings : itemSettingsList) {
            if (itemSettings.getBlockActionResult()) {
                ignoredItemTypeIds.add(itemSettings.getItemTypeId());
            }
        }

        // add all impossible types
        List<RulItemTypeExt> outputItemTypes = ruleService.getOutputItemTypes(output);
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

        // kontrola validity typu a specifikace
        StaticDataProvider sdp = this.staticDataService.getData();
        FundContext fundContext = FundContext.newInstance(fundVersion.getFund(), arrangementService, sdp);
        itemService.checkValidTypeAndSpec(fundContext, outputItem);

        int maxPosition = outputItemRepository.findMaxItemPosition(outputItem.getItemType(), outputItem.getOutput());

        if (outputItem.getPosition() == null || outputItem.getPosition() > maxPosition) {
            outputItem.setPosition(maxPosition + 1);
        } else {
            // find items which must be moved up
            List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(outputItem.getItemType(),
                    outputItem.getOutput(), outputItem.getPosition() - 1);
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
     * Deletes all output items by type for specified output and fund version.
     *
     * @return Count of affected items
     */
    @Transactional(TxType.MANDATORY)
    public int deleteOutputItemsByType(ArrFundVersion fundVersion,
                                       ArrOutput output,
                                       Integer itemTypeId,
                                       ArrChange deleteChange) {
        Validate.notNull(output);
        Validate.notNull(itemTypeId);
        Validate.notNull(deleteChange);

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Output items cannot be deleted in closed fund version.",
                    ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsByItemType(itemTypeId, output);

        for (ArrOutputItem item : outputItems) {
            item.setDeleteChange(deleteChange); // saved by commit
            publishOutputItemChanged(item, fundVersion.getFundVersionId());
        }

        return outputItems.size();
    }

    /**
     * Checks if recommended bulk actions are up-to-date for specified output.
     */
    private OutputRequestStatus checkRecommendedActions(ArrOutput output, ArrFundVersion fundVersion) {

        // find recommended actions
        List<RulAction> recommendedActions = actionRepository.findByRecommendedActionOutputType(output.getOutputType());
        if (recommendedActions.isEmpty()) {
            return OutputRequestStatus.OK;
        }

        // find output node ids
        List<Integer> nodeIds = getOutputNodes(output, fundVersion.getLockChange()).stream().map(n -> n.getNodeId()).collect(Collectors.toList());

        // find finished actions
        List<ArrBulkActionRun> finishedActions = bulkActionRunRepository.findBulkActionsByNodes(fundVersion.getFundVersionId(), nodeIds, State.FINISHED);
        if (recommendedActions.size() > finishedActions.size()) {
            return OutputRequestStatus.RECOMMENDED_ACTION_NOT_RUN;
        }

        ArrChange fromChange = null;
        Map<String, ArrChange> lastChangeByCode = new HashMap<>();
        for (ArrBulkActionRun finishedAction : finishedActions) {
            ArrChange change = finishedAction.getChange();
            ArrChange newestChange = lastChangeByCode.get(finishedAction.getBulkActionCode());
            // replace if a newer change found
            if (newestChange == null || change.getChangeDate().isAfter(newestChange.getChangeDate())) {
                lastChangeByCode.put(finishedAction.getBulkActionCode(), change);
                // replace to find the oldest of the last of every code
                if (fromChange == null || change.getChangeDate().isBefore(fromChange.getChangeDate())) {
                    fromChange = change;
                }
            }
        }

        for (RulAction ra : recommendedActions) {
            // test: bulk action not started yet
            if (!lastChangeByCode.containsKey(ra.getCode())) {
                return OutputRequestStatus.RECOMMENDED_ACTION_NOT_RUN;
            }
        }

        if (fromChange != null) {
            List<Integer> changeIdList = revertingChangesService.findChangesAfter(fundVersion.getFundId(), null, fromChange.getChangeId());
            HashSet<Integer> changeIdSet = new HashSet<>(changeIdList);
            for (ArrBulkActionRun finishedAction : finishedActions) {
                changeIdSet.remove(finishedAction.getChange().getChangeId());
            }
            for (ArrOutputResult outputResult : outputResultRepository.findByOutput(output)) {
                changeIdSet.remove(outputResult.getChange().getChangeId());
            }
            if (!changeIdSet.isEmpty()) {
                return OutputRequestStatus.DETECT_CHANGE;
            }
        }

        return OutputRequestStatus.OK;
    }

    public List<ApScopeVO> getRestrictedScopeVOs(ArrOutput output) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ApScope> scopeList = getRestrictedScopes(output);

        List<ApScopeVO> scopeVOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(scopeList)) {
            for (ApScope restrictionScope : scopeList) {
                scopeVOList.add(ApScopeVO.newInstance(restrictionScope, sdp));
            }
        }

        return scopeVOList;
    }

    public List<ApScope> getRestrictedScopes(ArrOutput output) {
        List<ArrOutputRestrictionScope> restrictionScopeList = outputRestrictionScopeRepository.findByOutput(output.getOutputId());

        List<ApScope> scopeList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(restrictionScopeList)) {
            for (ArrOutputRestrictionScope restrictionScope : restrictionScopeList) {
                scopeList.add(restrictionScope.getScope());
            }
        }
        return scopeList;
    }

    public List<ArrOutputItem> restrictItemsByScopes(ArrOutput output, List<ArrOutputItem> outputItems) {
        List<ApScope> restrictedScopes = getRestrictedScopes(output);
        List<ArrOutputItem> restrictedOutputItems = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(outputItems)) {
            for (ArrOutputItem outputItem : outputItems) {
                ArrData data = outputItem.getData();
                if (data instanceof ArrDataRecordRef) {
                    ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) data;
                    if (dataRecordRef.getRecord() != null) {
                        ApAccessPoint accessPoint = dataRecordRef.getRecord();
                        ApState state = stateRepository.findLastByAccessPoint(accessPoint);
                        if (state == null) {
                            throw new ObjectNotFoundException("Stav pro přístupový bod neexistuje", BaseCode.INVALID_STATE)
                                    .set("accessPointId", accessPoint.getAccessPointId());
                        }
                        ApScope scope = state.getScope();
                        if (isScopeRestricted(scope, restrictedScopes)) {
                            if (output.getAnonymizedAp() != null) {
                                dataRecordRef.setRecord(output.getAnonymizedAp());
                                restrictedOutputItems.add(outputItem);
                            }
                        } else {
                            restrictedOutputItems.add(outputItem);
                        }
                    }
                } else {
                    restrictedOutputItems.add(outputItem);
                }
            }
        }

        return restrictedOutputItems;
    }

    private boolean isScopeRestricted(ApScope scope, List<ApScope> restrictedScopes) {
        if (CollectionUtils.isNotEmpty(restrictedScopes)) {
            for (ApScope restrictedScope : restrictedScopes) {
                if (restrictedScope.getScopeId().equals(scope.getScopeId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create output templates
     * @param outputContext
     * @param templateIds
     * @return 
     */
	public List<ArrOutputTemplate> createOutputTemplates(ArrOutput output, Collection<Integer> templateIds) {
		List<RulTemplate> templates = templateRepository.findAllById(templateIds);
    	
    	Validate.isTrue(templateIds.size()==templates.size(), "Incorrect templateId");
    	
    	List<ArrOutputTemplate> outputTemplates = new ArrayList<>();
    	
    	for(RulTemplate template: templates) {

        	ArrOutputTemplate outputTemplate = new ArrOutputTemplate();
        	outputTemplate.setOutput(output);
        	outputTemplate.setTemplate(template);
                        
            outputTemplates.add(outputTemplate);
    	}
    	List<ArrOutputTemplate> saved = outputTemplateRepository.saveAll(outputTemplates);
		
    	return saved;
	}

	/**
	 * Return list of templates
	 * @param output
	 * @return
	 */
	public List<ArrOutputTemplate> getOutputTemplates(ArrOutput output) {
		return outputTemplateRepository.findAllByOutputFetchTemplate(output);
	}

    /**
     * Odstraneni vysledku
     * 
     * @param output
     */
    public void deleteOutputResults(ArrOutput output) {
        List<ArrOutputResult> outputResults = output.getOutputResults();
        for (ArrOutputResult outputResult : outputResults) {
            List<ArrOutputFile> outputFiles = outputResult.getOutputFiles();
            if (outputFiles != null && !outputFiles.isEmpty()) {
                dmsService.deleteFilesAfterCommit(outputFiles);
                outputFileRepository.deleteAll(outputFiles);
            }
            outputResultRepository.delete(outputResult);
        }
        outputResults.clear();
    }
}
