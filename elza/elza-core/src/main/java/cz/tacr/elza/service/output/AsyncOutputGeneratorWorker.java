package cz.tacr.elza.service.output;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.xml.sax.SAXException;

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.exception.ExceptionResponse;
import cz.tacr.elza.exception.ExceptionResponseBuilder;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.OutputCode;
import cz.tacr.elza.repository.OutputTemplateRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.FundLevelServiceInternal;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.output.generator.OutputGenerator;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

@Component
@Scope("prototype")
public class AsyncOutputGeneratorWorker implements IAsyncWorker {

    private final static Logger logger = LoggerFactory.getLogger(AsyncOutputGeneratorWorker.class);

    private Long beginTime;

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
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private OutputTemplateRepository outputTemplateRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private SchemaManager schemaManager;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AsyncRequest request;

    public AsyncOutputGeneratorWorker(final List<AsyncRequest> requests) {
        if (CollectionUtils.isNotEmpty(requests)) {
            Validate.isTrue(requests.size() == 1, "Only single request processing is supported by this worker");
            this.request = requests.get(0);
        } else {
            this.request = null;
        }
    }

    @Override
    public void run() {
        running.set(true);
        beginTime = System.currentTimeMillis();
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                generateOutput(request.getOutputId(), request.getUserId());
                return null;
            });
        } catch (Throwable t) {
            new TransactionTemplate(transactionManager).execute(status -> {
                handleException(t);
                return null;
            });
        } finally {
            running.set(false);
        }
    }

    @Override
    public AsyncRequest getRequest() {
        return request;
    }

    @Override
    public List<AsyncRequest> getRequests() {
        return Collections.singletonList(request);
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
     * Generování výstupu
     *
     * @param outputId
     * @param userId
     */
    private void generateOutput(Integer outputId, Integer userId) {
        ArrOutput output = outputServiceInternal.getOutputForGenerator(outputId);
        List<ArrOutputTemplate> templates = outputTemplateRepository.findAllByOutputFetchTemplate(output);

        // Drop any old output
        outputServiceInternal.deleteOutputResults(output);

        SecurityContext secCtx = userService.createSecurityContext(userId);
        SecurityContextHolder.setContext(secCtx);

        OutputParams params = createOutputParams(output);

        Path outputFilter = resourcePathResolver.getOutputFilter(output.getOutputFilter());
        params.setOutputFilter(outputFilter);

        for (ArrOutputTemplate template : templates) {
        	setOutputParamsTemplate(params, template);
	        Engine engine = template.getTemplate().getEngine();
	        try (OutputGenerator generator = outputGeneratorFactory.createOutputGenerator(engine)) {
	            generator.init(params);
	            ArrOutputResult result = generator.generate();
	            String validationSchema = template.getTemplate().getValidationSchema();
	            if (validationSchema != null) {
	            	validate(validationSchema, result);
	            }
	        } catch (IOException e) {
	            throw new SystemException("Failed to generate output", e, BaseCode.INVALID_STATE);
	        }
        }
        // reset error
        output.setError(null);
        OutputState state = resolveEndState(params);
        output.setState(state); // saved by commit

        outputServiceInternal.publishOutputStateChanged(output, request.getFundVersionId());
        eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
    }

    private void validate(String validationSchema, ArrOutputResult result) {
        if (!CollectionUtils.isEmpty(result.getOutputFiles())) {
            for (ArrOutputFile file : result.getOutputFiles()) {
                try (FileInputStream fis = new FileInputStream(resourcePathResolver.getDmsFile(String.valueOf(file.getFileId())).toString())) {
                    Schema schema = schemaManager.getSchema(validationSchema);
                    Validator validator = schema.newValidator();
                    validator.validate(new StreamSource(fis));
                  } catch (SAXException | IOException e) {
                      throw new SystemException("Failed to validate file", e, OutputCode.INVALID_FORMAT);
                  }
            }
        }
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
        ArrFundVersion fundVersion = em.find(ArrFundVersion.class, request.getFundVersionId());
        if (fundVersion == null) {
            throw new SystemException("Fund version for output not found", BaseCode.ID_NOT_EXIST).set("fundVersionId",
                    request.getFundVersionId());
        }

        ArrChange change = arrangementInternalService.createChange(Type.GENERATE_OUTPUT);

        List<ArrNodeOutput> outputNodes = outputServiceInternal.getOutputNodes(output, fundVersion.getLockChange());
        List<Integer> nodeIds = outputNodes.stream().map(no -> no.getNodeId()).collect(Collectors.toList());

        List<ArrOutputItem> outputItems = outputServiceInternal.getOutputItems(output, fundVersion.getLockChange());
        //omezení
        List<ArrOutputItem> restrictedItems = outputServiceInternal.restrictItemsByScopes(output, outputItems);

        return new OutputParams(output, change, fundVersion, nodeIds, restrictedItems);
    }

    private void setOutputParamsTemplate(OutputParams outputParams, ArrOutputTemplate outputTemplate) {
        RulTemplate template = outputTemplate.getTemplate();
        Path templateDir = resourcePathResolver.getTemplateDir(template).toAbsolutePath();
        outputParams.setTemplate(template);
        outputParams.setTemplateDir(templateDir);
    }

    /**
     * Handle exception raised during output processing. Must be called in transaction.
     */
    private void handleException(Throwable t) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(t);
        builder.logError(logger);

        ArrOutput output = em.find(ArrOutput.class, request.getOutputId());
        if (output != null) {
            ExceptionResponse er = builder.build();
            output.setError(er.toJson());
            output.setState(OutputState.OPEN); // saved by commit
            outputServiceInternal.publishOutputFailed(output, request.getFundVersionId());
        }
        eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, t));
    }

    @Override
    public void terminate() {
        while (running.get()) {
            try {
                logger.info("Čekání na dokončení generování výstupu: {}", request.getOutputId());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Nothing to do with this -> simply finish
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsyncOutputGeneratorWorker that = (AsyncOutputGeneratorWorker) o;
        return request.equals(that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request);
    }

}
