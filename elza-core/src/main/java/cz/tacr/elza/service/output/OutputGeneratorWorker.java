package cz.tacr.elza.service.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.exception.ExceptionResponse;
import cz.tacr.elza.exception.ExceptionResponseBuilder;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelServiceInternal;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.output.generator.OutputGenerator;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

public class OutputGeneratorWorker implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(OutputGeneratorWorker.class);

    private final int outputDefinitionId;

    private final int fundVersionId;

    /* managed components */

    private final EntityManager em;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final OutputServiceInternal outputServiceInternal;

    private final ResourcePathResolver resourcePathResolver;

    private final FundLevelServiceInternal fundLevelServiceInternal;

    private final PlatformTransactionManager transactionManager;

    private final ArrangementService arrangementService;

    public OutputGeneratorWorker(int outputDefinitionId,
                                 int fundVersionId,
                                 EntityManager em,
                                 OutputGeneratorFactory outputGeneratorFactory,
                                 OutputServiceInternal outputServiceInternal,
                                 ResourcePathResolver resourcePathResolver,
                                 FundLevelServiceInternal fundLevelServiceInternal,
                                 PlatformTransactionManager transactionManager,
                                 ArrangementService arrangementService) {
        this.outputDefinitionId = outputDefinitionId;
        this.fundVersionId = fundVersionId;
        this.em = em;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.outputServiceInternal = outputServiceInternal;
        this.resourcePathResolver = resourcePathResolver;
        this.fundLevelServiceInternal = fundLevelServiceInternal;
        this.transactionManager = transactionManager;
        this.arrangementService = arrangementService;
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
     * @throws
     */
    private void generateOutput() {
        ArrOutputDefinition definition = outputServiceInternal.getOutputDefinitionForGenerator(outputDefinitionId);

        Engine engine = definition.getTemplate().getEngine();
        try (OutputGenerator generator = outputGeneratorFactory.createOutputGenerator(engine);) {
            OutputParams params = createOutputParams(definition);
            generator.init(params);
            generator.generate();

            // reset error
            definition.setError(null);
            OutputState state = resolveEndState(params);
            definition.setState(state); // saved by commit
        } catch (IOException e) {
            throw new SystemException("Failed to generate output", e, BaseCode.INVALID_STATE);
        }

        outputServiceInternal.publishOutputStateChanged(definition, fundVersionId);
    }

    private OutputState resolveEndState(OutputParams params) {
        ArrChange change = params.getChange();
        for (Integer outputNodeId : params.getOutputNodeIds()) {
            if (!fundLevelServiceInternal.isLastChange(change, outputNodeId, false, true)) {
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

        ArrChange change = arrangementService.createChange(Type.GENERATE_OUTPUT);

        List<ArrNodeOutput> outputNodes = outputServiceInternal.getOutputNodes(definition, fundVersion.getLockChange());
        List<Integer> nodeIds = outputNodes.stream().map(no -> no.getNodeId()).collect(Collectors.toList());

        List<ArrOutputItem> outputItems = outputServiceInternal.getOutputItems(definition, fundVersion.getLockChange());

        Path templateDir = resourcePathResolver.getTemplateDir(definition.getTemplate()).toAbsolutePath();

        return new OutputParams(definition, change, fundVersion, nodeIds, outputItems, templateDir);
    }

    /**
     * Handle exception raised during output processing. Must be called in transaction.
     */
    private void handleException(Throwable t) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(t);
        builder.logError(logger);

        ArrOutputDefinition definition = em.find(ArrOutputDefinition.class, outputDefinitionId);
        if (definition != null) {
            ExceptionResponse er = builder.build();
            definition.setError(er.toJson());
            definition.setState(OutputState.OPEN); // saved by commit
        }
        outputServiceInternal.publishOutputFailed(definition, fundVersionId);
    }
}
