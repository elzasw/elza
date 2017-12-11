package cz.tacr.elza.service.output;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.output.generators.OutputGeneratorFactory;

@Service
public class OutputGeneratorService {

    private final static Logger logger = LoggerFactory.getLogger(OutputGeneratorService.class);

    private final OutputGeneratorQueue queue;

    private final PlatformTransactionManager transactionManager;

    private final FundVersionRepository fundVersionRepository;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final IEventNotificationService eventNotificationService;

    private final ArrangementService arrangementService;

    private final OutputService outputService;

    private final BulkActionService bulkActionService;

    private final OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    public OutputGeneratorService(@Qualifier("threadPoolTaskExecutorOG") ThreadPoolTaskExecutor taskExecutor,
                                  PlatformTransactionManager transactionManager,
                                  FundVersionRepository fundVersionRepository,
                                  OutputGeneratorFactory outputGeneratorFactory,
                                  IEventNotificationService eventNotificationService,
                                  ArrangementService arrangementService,
                                  OutputService outputService,
                                  BulkActionService bulkActionService,
                                  OutputDefinitionRepository outputDefinitionRepository) {
        this.queue = new OutputGeneratorQueue(taskExecutor);
        this.transactionManager = transactionManager;
        this.fundVersionRepository = fundVersionRepository;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.eventNotificationService = eventNotificationService;
        this.arrangementService = arrangementService;
        this.outputService = outputService;
        this.bulkActionService = bulkActionService;
        this.outputDefinitionRepository = outputDefinitionRepository;
    }

    public void init() {
        queue.startDispatcher();
    }

    /**
     * Add request for output generation.
     *
     * @param outputDefinitionId
     * @param fundVersionId
     */
    @Transactional(TxType.MANDATORY)
    public OutputRequestStatus addRequest(int outputDefinitionId, ArrFundVersion fundVersion, boolean checkBulkActions) {
        ArrOutputDefinition definition = getOpenOutputDefinition(outputDefinitionId);

        OutputRequestStatus status = OutputRequestStatus.OK;
        if (checkBulkActions) {
            status = checkBulkActions(definition, fundVersion);
            if (status != OutputRequestStatus.OK) {
                return status;
            }
        }

        definition.setState(OutputState.GENERATING); // saved by commit

        OutputGeneratorWorker worker = new OutputGeneratorWorker(outputDefinitionId, fundVersion.getFundVersionId(),
                fundVersionRepository, outputGeneratorFactory, eventNotificationService, arrangementService, outputService,
                outputDefinitionRepository, transactionManager);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    queue.addWorker(worker);
                } else {
                    logger.warn("Request for output will be terminated due to rollbacked source transaction, outputDefinitionId:{}",
                            worker.getOutputDefinitionId());
                }
            }
        });

        return status;
    }

    private ArrOutputDefinition getOpenOutputDefinition(int outputDefinitionId) {
        ArrOutputDefinition definition = outputDefinitionRepository.findOne(outputDefinitionId);
        if (definition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }
        if (definition.getState() != OutputState.OPEN) {
            throw new SystemException("Invalid output definition state", BaseCode.INVALID_STATE)
                    .set("expectedState", OutputState.OPEN).set("givenState", definition.getState());
        }
        return definition;
    }

    /**
     * Checks if recommended bulk actions are up-to-date for specified output definition.
     *
     * @param definition
     * @param fundVersion
     * @return
     */
    private OutputRequestStatus checkBulkActions(ArrOutputDefinition definition, ArrFundVersion fundVersion) {
        bulkActionService.checkOutdatedActions(fundVersion.getFundVersionId());

        // find output node ids
        List<ArrNodeOutput> outputNodes = outputService.getOutputNodes(definition, fundVersion.getLockChange());
        List<Integer> outputNodeIds = new ArrayList<>(outputNodes.size());
        outputNodes.forEach(n -> outputNodeIds.add(n.getNodeId()));

        // find recommended actions
        List<RulAction> recommendedActions = bulkActionService.getRecommendedActions(definition.getOutputType());
        if (recommendedActions.isEmpty()) {
            return OutputRequestStatus.OK;
        }

        // find finished actions
        List<ArrBulkActionRun> finishedAction = bulkActionService.findBulkActionsByNodes(fundVersion.getFundVersionId(),
                outputNodeIds);
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
                if (!arrangementService.isLastChange(newestChange, nodeId, false, true)) {
                    return OutputRequestStatus.DETECT_CHANGE;
                }
            }
        }

        return OutputRequestStatus.OK;
    }
}
