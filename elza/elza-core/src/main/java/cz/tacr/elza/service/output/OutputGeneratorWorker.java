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
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
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

    private final int outputId;

    private final int fundVersionId;

    /* managed components */

    private final EntityManager em;

    private final OutputGeneratorFactory outputGeneratorFactory;

    private final OutputServiceInternal outputServiceInternal;

    private final ResourcePathResolver resourcePathResolver;

    private final FundLevelServiceInternal fundLevelServiceInternal;

    private final PlatformTransactionManager transactionManager;

    private final ArrangementService arrangementService;

    public OutputGeneratorWorker(int outputId,
                                 int fundVersionId,
                                 EntityManager em,
                                 OutputGeneratorFactory outputGeneratorFactory,
                                 OutputServiceInternal outputServiceInternal,
                                 ResourcePathResolver resourcePathResolver,
                                 FundLevelServiceInternal fundLevelServiceInternal,
                                 PlatformTransactionManager transactionManager,
                                 ArrangementService arrangementService) {
        this.outputId = outputId;
        this.fundVersionId = fundVersionId;
        this.em = em;
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.outputServiceInternal = outputServiceInternal;
        this.resourcePathResolver = resourcePathResolver;
        this.fundLevelServiceInternal = fundLevelServiceInternal;
        this.transactionManager = transactionManager;
        this.arrangementService = arrangementService;
    }

    public int getOutputId() {
        return outputId;
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
        ArrOutput output = outputServiceInternal.getOutputForGenerator(outputId);

        Engine engine = output.getTemplate().getEngine();
        try (OutputGenerator generator = outputGeneratorFactory.createOutputGenerator(engine);) {
            OutputParams params = createOutputParams(output);
            generator.init(params);
            generator.generate();

            // reset error
            output.setError(null);
            OutputState state = resolveEndState(params);
            output.setState(state); // saved by commit
        } catch (IOException e) {
            throw new SystemException("Failed to generate output", e, BaseCode.INVALID_STATE);
        }

        outputServiceInternal.publishOutputStateChanged(output, fundVersionId);
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

    private OutputParams createOutputParams(ArrOutput output) {
        ArrFundVersion fundVersion = em.find(ArrFundVersion.class, fundVersionId);
        if (fundVersion == null) {
            throw new SystemException("Fund version for output not found", BaseCode.ID_NOT_EXIST).set("fundVersionId",
                    fundVersionId);
        }

        ArrChange change = arrangementService.createChange(Type.GENERATE_OUTPUT);

        List<ArrNodeOutput> outputNodes = outputServiceInternal.getOutputNodes(output, fundVersion.getLockChange());
        List<Integer> nodeIds = outputNodes.stream().map(no -> no.getNodeId()).collect(Collectors.toList());

        List<ArrOutputItem> outputItems = outputServiceInternal.getOutputItems(output, fundVersion.getLockChange());
        //omezení
        List<ArrOutputItem> restrictedItems = outputServiceInternal.restrictItemsByScopes(output, outputItems);

        Path templateDir = resourcePathResolver.getTemplateDir(output.getTemplate()).toAbsolutePath();

        return new OutputParams(output, change, fundVersion, nodeIds, restrictedItems, templateDir);
    }

    /**
     * Handle exception raised during output processing. Must be called in transaction.
     */
    private void handleException(Throwable t) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(t);
        builder.logError(logger);

        ArrOutput output = em.find(ArrOutput.class, outputId);
        if (output != null) {
            ExceptionResponse er = builder.build();
            output.setError(er.toJson());
            output.setState(OutputState.OPEN); // saved by commit
        }
        outputServiceInternal.publishOutputFailed(output, fundVersionId);
    }
}
