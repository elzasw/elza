package cz.tacr.elza.service.output;

import java.nio.file.Path;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.FundLevelServiceInternal;
import cz.tacr.elza.service.output.generator.OutputGenerator;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

public class OutputGeneratorWorker implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(OutputGeneratorWorker.class);

    private final int outputDefinitionId;

    private final int fundVersionId;

    private final Integer userId;

    /* managed components */

    private final EntityManager em;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final OutputGeneratorService outputGeneratorService;

    private final ResourcePathResolver resourcePathResolver;

    private final FundLevelServiceInternal fundLevelServiceInternal;

    private final PlatformTransactionManager transactionManager;

    public OutputGeneratorWorker(int outputDefinitionId,
                                 int fundVersionId,
                                 Integer userId,
                                 EntityManager em,
                                 OutputGeneratorFactory outputGeneratorFactory,
                                 OutputGeneratorService outputGeneratorService,
                                 ResourcePathResolver resourcePathResolver,
                                 FundLevelServiceInternal fundLevelServiceInternal,
                                 PlatformTransactionManager transactionManager) {
        this.outputDefinitionId = outputDefinitionId;
        this.fundVersionId = fundVersionId;
        this.userId = userId;
        this.em = em;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.outputGeneratorService = outputGeneratorService;
        this.resourcePathResolver = resourcePathResolver;
        this.fundLevelServiceInternal = fundLevelServiceInternal;
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
        ArrOutputDefinition definition = outputGeneratorService.getOutputDefinitionForGenerator(outputDefinitionId);

        Engine engine = definition.getTemplate().getEngine();
        OutputGenerator generator = outputGeneratorFactory.createOutputGenerator(engine);

        OutputParams params = createOutputParams(definition);
        generator.init(params);
        generator.generate();

        OutputState state = resolveEndState(params);
        definition.setState(state); // saved by commit

        outputGeneratorService.publishOutputStateChanged(definition, fundVersionId);
    }

    private OutputState resolveEndState(OutputParams params) {
        ArrChange change = params.getChange();
        for (ArrNodeOutput outputNode : params.getOutputNodes()) {
            if (!fundLevelServiceInternal.isLastChange(change, outputNode.getNodeId(), false, true)) {
                return OutputState.OUTDATED;
            }
        }
        return OutputState.FINISHED;
    }

    private OutputParams createOutputParams(ArrOutputDefinition definition) {
        ArrFundVersion fundVersion = em.find(ArrFundVersion.class, fundVersionId);
        if (fundVersion == null) {
            throw new SystemException("Fund version for output not found", BaseCode.ID_NOT_EXIST).set("fundVersionId",
                    fundVersionId);
        }

        ArrChange change = outputGeneratorService.createChange(userId);

        List<ArrNodeOutput> outputNodes = outputGeneratorService.getOutputNodes(definition, fundVersion.getLockChange());

        List<ArrOutputItem> outputItems = outputGeneratorService.getOutputItems(definition, fundVersion.getLockChange());

        Path templateDir = resourcePathResolver.getTemplateDirectory(definition.getTemplate());

        return new OutputParams(definition, change, fundVersion, outputNodes, outputItems, templateDir);
    }

    /**
     * Handle exception raised during output processing. Must be called in transaction.
     */
    private void handleException(Throwable t) {
        ArrOutputDefinition definition = em.find(ArrOutputDefinition.class, outputDefinitionId);
        if (definition != null) {
            definition.setError(getCauseMessages(t, 1000));
            definition.setState(OutputState.OPEN); // saved by commit
        } else {
            logger.error("Output generator worker failed, outputDefinitionId:" + outputDefinitionId, t);
        }
        outputGeneratorService.publishOutputFailed(definition, fundVersionId);
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
}
