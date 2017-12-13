package cz.tacr.elza.service.output;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventIdAndStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

@Service
public class OutputGeneratorService {

    private final static Logger logger = LoggerFactory.getLogger(OutputGeneratorService.class);

    private static final String PACKAGE_TEMPLATES_DIR = "templates";

    private final OutputQueueManager queueManager = new OutputQueueManager(2);

    private final PlatformTransactionManager transactionManager;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final IEventNotificationService eventNotificationService;

    private final ArrangementService arrangementService;

    private final BulkActionService bulkActionService;

    private final OutputDefinitionRepository outputDefinitionRepository;

    private final NodeOutputRepository nodeOutputRepository;

    private final OutputItemRepository outputItemRepository;

    private final EntityManager em;

    private final StaticDataService staticDataService;

    private final String rulesDirectory;

    @Autowired
    public OutputGeneratorService(PlatformTransactionManager transactionManager,
                                  OutputGeneratorFactory outputGeneratorFactory,
                                  IEventNotificationService eventNotificationService,
                                  ArrangementService arrangementService,
                                  BulkActionService bulkActionService,
                                  OutputDefinitionRepository outputDefinitionRepository,
                                  NodeOutputRepository nodeOutputRepository,
                                  OutputItemRepository outputItemRepository,
                                  EntityManager em,
                                  StaticDataService staticDataService,
                                  @Value("${elza.rulesDir}") String rulesDirectory) {
        this.transactionManager = transactionManager;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.eventNotificationService = eventNotificationService;
        this.arrangementService = arrangementService;
        this.bulkActionService = bulkActionService;
        this.outputDefinitionRepository = outputDefinitionRepository;
        this.nodeOutputRepository = nodeOutputRepository;
        this.outputItemRepository = outputItemRepository;
        this.em = em;
        this.staticDataService = staticDataService;
        this.rulesDirectory = rulesDirectory;
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
        queueManager.start();
    }

    /**
     * Searches nodes connected to output definition. Nodes must be valid by specified lock change.
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
    public List<ArrOutputItem> getDirectOutputItems(ArrOutputDefinition outputDefinition, ArrChange lockChange) {
        Validate.notNull(outputDefinition);

        if (lockChange == null) {
            return outputItemRepository.findByOutputAndDeleteChangeIsNull(outputDefinition);
        }
        return outputItemRepository.findByOutputAndChange(outputDefinition, lockChange);
    }

    @Transactional(TxType.MANDATORY)
    public void publishOutputStateChanged(ArrOutputDefinition outputDefinition, int fundVersionId) {
        String outputState = outputDefinition.getState().name();
        EventIdAndStringInVersion stateChangedEvent = EventFactory.createStringAndIdInVersionEvent(EventType.OUTPUT_STATE_CHANGE,
                fundVersionId, outputDefinition.getOutputDefinitionId(), outputState);
        eventNotificationService.publishEvent(stateChangedEvent);
    }

    /**
     * Locates system directory for specified template. Directory must exist.
     *
     * @return Absolute path for template directory.
     */
    @Transactional
    public Path getTemplateDirectory(RulTemplate template) {
        RulPackage rulPackage = staticDataService.getData().getPackageById(template.getPackageId());

        String packageDir = rulPackage.getCode();
        String templateSubDir = template.getDirectory();

        Path dirPath = Paths.get(rulesDirectory, packageDir, PACKAGE_TEMPLATES_DIR, templateSubDir);
        if (!Files.isDirectory(dirPath)) {
            throw new SystemException("Template directory not found, path:" + dirPath);
        }
        return dirPath.toAbsolutePath();
    }

    /**
     * Creates new change for output.
     *
     * @param userId can be null for system administrator
     */
    @Transactional(TxType.MANDATORY)
    public ArrChange createChange(Integer userId) {
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
                                          Integer userId,
                                          boolean checkBulkActions) {
        // find open output definition
        ArrOutputDefinition definition = outputDefinitionRepository.findOne(outputDefinitionId);
        if (definition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }
        if (definition.getState() != OutputState.OPEN) {
            throw new SystemException("Requested output must be in OPEN state", BaseCode.INVALID_STATE)
                    .set("expectedState", OutputState.OPEN).set("givenState", definition.getState());
        }

        // check recommended bulk action
        if (checkBulkActions) {
            OutputRequestStatus status = checkBulkActions(definition, fundVersion);
            if (status != OutputRequestStatus.OK) {
                return status;
            }
        }

        // save generating state only when caller transaction is committed
        definition.setState(OutputState.GENERATING);

        // create worker
        OutputGeneratorWorker worker = new OutputGeneratorWorker(outputDefinitionId, fundVersion.getFundVersionId(), userId, em,
                outputGeneratorFactory, eventNotificationService, arrangementService, this, transactionManager);

        // register after commit action
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    queueManager.addWorker(worker);
                } else {
                    logger.warn("Request for output is cancelled due to rollbacked source transaction, outputDefinitionId:{}",
                            worker.getOutputDefinitionId());
                }
            }
        });

        return OutputRequestStatus.OK;
    }

    /**
     * Searches definition in generating state with fetches for output generator.
     *
     * @throws SystemException When definition not found or it has invalid state.
     */
    @Transactional
    public ArrOutputDefinition getOutputDefinitionForGenerator(int outputDefinitionId) {
        ArrOutputDefinition definition = outputDefinitionRepository.findOneAndFetchForGenerator(outputDefinitionId);
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
     * Checks if recommended bulk actions are up-to-date for specified output definition.
     *
     * @param definition
     * @param fundVersion
     * @return
     */
    private OutputRequestStatus checkBulkActions(ArrOutputDefinition definition, ArrFundVersion fundVersion) {
        bulkActionService.checkOutdatedActions(fundVersion.getFundVersionId());

        // find output node ids
        List<ArrNodeOutput> outputNodes = getOutputNodes(definition, fundVersion.getLockChange());
        List<Integer> outputNodeIds = new ArrayList<>(outputNodes.size());
        outputNodes.forEach(n -> outputNodeIds.add(n.getNodeId()));

        // find recommended actions
        List<RulAction> recommendedActions = bulkActionService.getRecommendedActions(definition.getOutputType());
        if (recommendedActions.isEmpty()) {
            return OutputRequestStatus.OK;
        }

        // find finished actions
        List<ArrBulkActionRun> finishedAction = bulkActionService.findBulkActionsByNodes(fundVersion, outputNodeIds);
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
