package cz.tacr.elza.bulkaction;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.UserService;

@Component
@Scope("prototype")
public class AsyncBulkActionWorker implements IAsyncWorker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncBulkActionWorker.class);

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    protected BulkActionHelperService bulkActionHelperService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserService userService;

    /**
     * Hromadná akce
     */
    private BulkAction bulkAction;

    private Long beginTime;

    private final AsyncRequest request;

    /**
     * Seznam vstupních uzlů (podstromů AS)
     */
    private List<Integer> inputNodeIds;

    public AsyncBulkActionWorker(final List<AsyncRequest> requests) {
        if (CollectionUtils.isNotEmpty(requests)) {
            Validate.isTrue(requests.size() == 1, "Only single request processing is supported by this worker");
            this.request = requests.get(0);
        } else {
            this.request = null;
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
    public void run() {
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                beginTime = System.currentTimeMillis();
                ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
                bulkAction = bulkActionHelperService.prepareToRun(bulkActionRun);
                inputNodeIds = bulkActionHelperService.getBulkActionNodeIds(bulkActionRun);
                logger.info("Bulk action started: {}", this);

                // start action - mark it as running
                bulkActionRun.setDateStarted(new Date());
                bulkActionRun.setState(ArrBulkActionRun.State.RUNNING);
                bulkActionHelperService.updateAction(bulkActionRun);
                return null;
            });
        } catch (Throwable e) {
            logger.error("Failed to start action: {}", this, e);
            try {
                new TransactionTemplate(transactionManager).execute(status -> {
                    handleException(e);
                    return null;
                });
            } catch (Throwable eI) {
                logger.error("Failed to handle exception: ", eI);
                throw eI;
            }
        }

        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                executeInTransaction();
                logger.info("Bulk action succesfully finished: {}", this);
                return null;
            });
        } catch (Throwable e) {
            logger.error("Bulk action failed, action: " + this + ", error: ", e);
            try {
                new TransactionTemplate(transactionManager).execute(status -> {
                    handleException(e);
                    return null;
                });
            } catch (Throwable eI) {
                logger.error("Failed to handle exception: ", eI);
                throw eI;
            }

        }
        //return this;
    }

    private void executeInTransaction() {
        // prepare sec context
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
        SecurityContext ctx = userService.createSecurityContext(bulkActionRun.getUserId());
        SecurityContextHolder.setContext(ctx);

        try {

            // prepare context object
            ActionRunContext runContext = new ActionRunContext(inputNodeIds, bulkActionRun);

            bulkAction.execute(runContext);

            // TODO: Add check that action was not interrupted
            bulkActionRun.setDateFinished(new Date());
            bulkActionRun.setState(ArrBulkActionRun.State.FINISHED);
            bulkActionHelperService.updateAction(bulkActionRun);
            bulkActionHelperService.onFinished(bulkActionRun);

        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
            eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
        }
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

    private void handleException(Throwable e) {
        ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
        bulkActionRun.setError(e.getLocalizedMessage());
        bulkActionRun.setState(ArrBulkActionRun.State.ERROR);

        // protože hromadná akce skončila chybou vrátíme výstup do původního stavu
        List<Integer> nodeIds = bulkActionHelperService.getBulkActionNodeIds(bulkActionRun);        
        outputServiceInternal.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                                                        nodeIds,
                                                        ArrOutput.OutputState.OPEN,
                                                        ArrOutput.OutputState.COMPUTING);

        bulkActionHelperService.updateAction(bulkActionRun);
        eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, e));
    }

    /**
     * Ukončí běžící hromadnou akci.
     */
    public void terminate() {
        ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
        if (!bulkActionRun.setInterrupted(true)) {
            return;
        }

        try {
            while (bulkActionRun.getState() == ArrBulkActionRun.State.RUNNING) {
                try {
                    logger.info("Čekání na dokončení hromadné akce: {}", request);
                    Thread.sleep(100);
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    bulkActionRun = transactionTemplate.execute(status -> bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId()));
                } catch (InterruptedException e) {
                    // Nothing to do with this -> simply finish
                    Thread.currentThread().interrupt();
                }
            }

            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);

        } finally {
            bulkActionRun.setInterrupted(false);
            bulkActionHelperService.updateAction(bulkActionRun);
        }
    }

    @Override
    public String toString() {
        return "BulkActionWorker{" +
                "bulkAction=" + bulkAction +
                ", versionId=" + request.getFundVersionId() +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsyncBulkActionWorker that = (AsyncBulkActionWorker) o;
        return request.equals(that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request);
    }
}
