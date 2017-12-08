package cz.tacr.elza.service.output.dev;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdAndStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.dev.generators.OutputGenerator;
import cz.tacr.elza.service.output.dev.generators.OutputGeneratorFactory;

public class OutputGeneratorWorker implements Runnable {

    protected final Integer outputDefinitionId;

    protected final Integer fundVersionId;

    protected final boolean checkBulkActions;

    /* managed components */

    private final PlatformTransactionManager transactionManager;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final OutputDefinitionRepository outputDefinitionRepository;

    private final FundVersionRepository fundVersionRepository;

    private final EventNotificationService eventNotificationService;

    public OutputGeneratorWorker(Integer outputDefinitionId,
                                 Integer fundVersionId,
                                 boolean checkBulkActions,
                                 PlatformTransactionManager transactionManager,
                                 OutputGeneratorFactory outputGeneratorFactory,
                                 OutputDefinitionRepository outputDefinitionRepository,
                                 FundVersionRepository fundVersionRepository,
                                 EventNotificationService eventNotificationService) {
        this.outputDefinitionId = Validate.notNull(outputDefinitionId);
        this.fundVersionId = Validate.notNull(fundVersionId);
        this.checkBulkActions = checkBulkActions;
        this.transactionManager = transactionManager;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.outputDefinitionRepository = outputDefinitionRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.eventNotificationService = eventNotificationService;
    }

    @Override
    public void run() {
        try {
            executeWithinTransaction(() -> generate());
        } catch (Throwable t) {
            executeWithinTransaction(() -> handleException(t));
        }
    }

    private void generate() {
        ArrOutputDefinition definition = getOutputDefinition();
        if (definition.getState() != OutputState.OPEN) {
            throw new SystemException("Cannot generate output in state " + definition.getState(), BaseCode.INVALID_STATE);
        }

        OutputParams params = createOutputParams(definition);

        if (checkBulkActions) {
            // TODO: check recommended actions, if not up-to-date call fireOutputInterrupted and
            // stop processing
        }

        Engine engine = definition.getTemplate().getEngine();
        OutputGenerator generator = outputGeneratorFactory.createOutputGenerator(engine);

        generator.init(params);
        generator.generate();

        definition.setState(OutputState.FINISHED);
        fireOutputGenerated();
    }

    private OutputParams createOutputParams(ArrOutputDefinition definition) {
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        if (fundVersion == null) {
            throw new SystemException("Fund version for output not found", BaseCode.ID_NOT_EXIST).set("fundVersionId",
                    fundVersionId);
        }

        List<ArrNodeOutput> outputNodes = outputService.getOutputNodes(outputDefinition, fundVersion.getLockChange());

        List<ArrOutputItem> outputItems = outputService.getDirectOutputItems(outputDefinition, lockChange);

        return new OutputParams(definition, change, fundVersion, rootNodes, directItems);
    }

    private ArrOutputDefinition getOutputDefinition() {
        // fetch is directly used by template generators and indirectly (persistent context) by
        // DEXML generator
        ArrOutputDefinition definition = outputDefinitionRepository.findOneFetchTypeAndFundAndInstitution(outputDefinitionId);
        if (definition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }
        return definition;
    }

    private void handleException(Throwable t) {
        ArrOutputDefinition definition = outputDefinitionRepository.findOne(outputDefinitionId);
        definition.setError(getCauses(t));
        definition.setState(OutputState.OPEN);
        fireOutputInterrupted(OutputInterruptReason.ERROR);
    }

    private void fireOutputGenerated() {
        EventIdAndStringInVersion changeEvent = EventFactory.createStringAndIdInVersionEvent(EventType.OUTPUT_STATE_CHANGE,
                fundVersionId, outputDefinitionId, OutputState.FINISHED.name());
        eventNotificationService.forcePublish(changeEvent);
    }

    private void fireOutputInterrupted(OutputInterruptReason reason) {
        EventIdAndStringInVersion changeEvent = EventFactory.createStringAndIdInVersionEvent(EventType.OUTPUT_STATE_CHANGE,
                fundVersionId, outputDefinitionId, reason.name());
        eventNotificationService.forcePublish(changeEvent);
    }

    private void executeWithinTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).execute(status -> {
            action.run();
            return null;
        });
    }

    private static String getCauses(Throwable t) {
        final int capacity = 1000;
        final StringBuilder sb = new StringBuilder(capacity);
        while (t != null && sb.length() < capacity) {
            sb.append(t.getLocalizedMessage()).append("\n");
            t = t.getCause();
        }
        if (sb.length() > capacity) {
            sb.setLength(capacity - 3);
            sb.append("...");
        }
        return sb.toString();
    }
}
