package cz.tacr.elza.service.output;

import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.AsyncRequestVO;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.asynchactions.TimeRequestInfo;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.exception.ExceptionResponse;
import cz.tacr.elza.exception.ExceptionResponseBuilder;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.FundLevelServiceInternal;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.output.generator.OutputGenerator;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class AsyncOutputGeneratorWorker implements IAsyncWorker {

    private final static Logger logger = LoggerFactory.getLogger(AsyncOutputGeneratorWorker.class);

    private final int outputId;

    private final int fundVersionId;

    private Long beginTime;

    private Long currentRequestId;

    @Autowired
    private EntityManager em;

    @Autowired
    private OutputGeneratorFactory outputGeneratorFactory;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private FundLevelServiceInternal fundLevelServiceInternal;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ArrangementService arrangementService;

    public AsyncOutputGeneratorWorker(final Integer fundVersionId,
                                      final Integer outputId,
                                      final Long asyncRequestId) {
        this.outputId = outputId;
        this.fundVersionId = fundVersionId;
        this.currentRequestId = asyncRequestId;
    }

    public int getOutputId() {
        return outputId;
    }

    @Override
    @Transactional
    public void run() {
        beginTime = System.currentTimeMillis();
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                generateOutput(outputId);
                return null;
            });
        } catch (Throwable t) {
            new TransactionTemplate(transactionManager).execute(status -> {
                handleException(t);
                return null;
            });
        }

        // return this;
    }

    @Override
    public Long getRequestId() {
        return currentRequestId;
    }

    @Override
    public Long getBeginTime() {
        return beginTime;
    }

    @Override
    public Long getRunningTime() {
        if (beginTime != null) {
            return System.currentTimeMillis() - beginTime;
        } else {
            return null;
        }
    }

    /**
     * Process output. Must be called in transaction.
     *
     * @throws
     */
    private void generateOutput(Integer outputId) {
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
        eventPublisher.publishEvent(new AsyncRequestEvent(resultEvent()));
    }

    private AsyncRequestVO resultEvent() {
        AsyncRequestVO publish = new AsyncRequestVO();
        publish.setType(AsyncTypeEnum.OUTPUT);
        publish.setFundVersionId(fundVersionId);
        publish.setRequestId(currentRequestId);
        publish.setOutputId(outputId);
        return publish;
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

        Path templateDir = resourcePathResolver.getTemplateDir(output.getTemplate()).toAbsolutePath();

        return new OutputParams(output, change, fundVersion, nodeIds, outputItems, templateDir);
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
        eventPublisher.publishEvent(new AsyncRequestEvent(resultEvent()));
    }

    @Override
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    public void terminate() {

    }

    @Override
    public Integer getCurrentId() {
        return outputId;
    }


}
