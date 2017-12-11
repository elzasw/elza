package cz.tacr.elza.service.output;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventIdAndStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.generators.OutputGenerator;
import cz.tacr.elza.service.output.generators.OutputGeneratorFactory;

public class OutputGeneratorWorker implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(OutputGeneratorWorker.class);

    private final static String ERROR_OUTPUT_STATE = "ERROR";

    private final int outputDefinitionId;

    private final int fundVersionId;

    /* managed components */

    private final FundVersionRepository fundVersionRepository;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final IEventNotificationService eventNotificationService;

    private final ArrangementService arrangementService;

    private final OutputService outputService;

    private final OutputDefinitionRepository outputDefinitionRepository;

    private final PlatformTransactionManager transactionManager;

    public OutputGeneratorWorker(int outputDefinitionId,
                                 int fundVersionId,
                                 FundVersionRepository fundVersionRepository,
                                 OutputGeneratorFactory outputGeneratorFactory,
                                 IEventNotificationService eventNotificationService,
                                 ArrangementService arrangementService,
                                 OutputService outputService,
                                 OutputDefinitionRepository outputDefinitionRepository,
                                 PlatformTransactionManager transactionManager) {
        this.outputDefinitionId = outputDefinitionId;
        this.fundVersionId = fundVersionId;
        this.fundVersionRepository = fundVersionRepository;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.eventNotificationService = eventNotificationService;
        this.arrangementService = arrangementService;
        this.outputService = outputService;
        this.outputDefinitionRepository = outputDefinitionRepository;
        this.transactionManager = transactionManager;
    }

    public int getOutputDefinitionId() {
        return outputDefinitionId;
    }

    @Override
    public void run() {
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                generateOutput();
                return null;
            });
        } catch (Throwable t) {
            new TransactionTemplate(transactionManager).execute(status -> {
                handleException(t);
                return null;
            });
        }
    }

    /**
     * Process output. Must be called in transaction.
     */
    private void generateOutput() {
        ArrOutputDefinition definition = getOutputDefinitionForGenerator();

        Engine engine = definition.getTemplate().getEngine();
        OutputGenerator generator = outputGeneratorFactory.createOutputGenerator(engine);

        OutputParams params = createOutputParams(definition);
        generator.init(params);
        generator.generate();

        OutputState state = resolveEndState(params);
        definition.setState(state); // saved by commit

        outputService.publishOutputStateChanged(definition, fundVersionId);
    }

    private ArrOutputDefinition getOutputDefinitionForGenerator() {
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

    private OutputState resolveEndState(OutputParams params) {
        ArrChange change = params.getChange();
        for (ArrNodeOutput outputNode : params.getOutputNodes()) {
            if (!arrangementService.isLastChange(change, outputNode.getNodeId(), false, true)) {
                return OutputState.OUTDATED;
            }
        }
        return OutputState.FINISHED;
    }

    private OutputParams createOutputParams(ArrOutputDefinition definition) {
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        if (fundVersion == null) {
            throw new SystemException("Fund version for output not found", BaseCode.ID_NOT_EXIST).set("fundVersionId",
                    fundVersionId);
        }

        ArrChange change = arrangementService.createChange(Type.GENERATE_OUTPUT);

        List<ArrNodeOutput> outputNodes = outputService.getOutputNodes(definition, fundVersion.getLockChange());

        List<ArrOutputItem> directItems = outputService.getDirectOutputItems(definition, fundVersion.getLockChange());

        return new OutputParams(definition, change, fundVersion, outputNodes, directItems);
    }

    /**
     * Handle exception raised during output processing. Must be called in transaction.
     */
    private void handleException(Throwable t) {
        ArrOutputDefinition definition = outputDefinitionRepository.findOne(outputDefinitionId);
        if (definition != null) {
            definition.setError(getCauseMessages(t, 1000));
            definition.setState(OutputState.OPEN); // saved by commit
        } else {
            logger.error("Output generator worker failed, outputDefinitionId:" + outputDefinitionId, t);
        }
        EventIdAndStringInVersion stateChangedEvent = EventFactory.createStringAndIdInVersionEvent(EventType.OUTPUT_STATE_CHANGE,
                fundVersionId, outputDefinitionId, ERROR_OUTPUT_STATE);
        eventNotificationService.publishEvent(stateChangedEvent);
    }

    private static String getCauseMessages(Throwable t, int charLimit) {
        final StringBuilder sb = new StringBuilder(charLimit);
        while (t != null && sb.length() < charLimit) {
            sb.append(t.getLocalizedMessage()).append("\n");
            t = t.getCause();
        }
        if (sb.length() > charLimit) {
            sb.setLength(charLimit - 3);
            sb.append("...");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputDefinitionId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof OutputGeneratorWorker) {
            OutputGeneratorWorker o = (OutputGeneratorWorker) obj;
            return outputDefinitionId == o.outputDefinitionId;
        }
        return false;
    }
}
