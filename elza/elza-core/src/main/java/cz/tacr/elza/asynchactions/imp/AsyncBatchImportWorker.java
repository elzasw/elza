package cz.tacr.elza.asynchactions.imp;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncRequest;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.service.ImpBatchService;
import cz.tacr.elza.service.UserService;

/**
 * Carries out one import batch. The batch is the unit here for the same reason it is the unit of
 * queueing: a batch is a coherent piece of work with its own progress and terminal state; its
 * items run one at a time, each in a transaction of its own, inside this single run.
 */
@Component
@Scope("prototype")
public class AsyncBatchImportWorker implements IAsyncWorker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncBatchImportWorker.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ImpBatchService batchService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final AsyncRequest request;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Long beginTime;

    public AsyncBatchImportWorker(final List<AsyncRequest> requests) {
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
        boolean success = false;
        Throwable failure = null;
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        try {
            if (request.getUserId() != null) {
                logger.debug("Import batch {} runs as user {}", request.getBatchId(), request.getUserId());
                SecurityContextHolder.setContext(userService.createSecurityContext(request.getUserId()));
            } else {
                logger.debug("Import batch {} runs as system admin (no user_id)", request.getBatchId());
                SecurityContextHolder.setContext(userService.createSecurityContextSystem());
            }
            batchService.runBatch(request.getBatchId());
            success = true;
        } catch (Throwable t) {
            failure = t;
            logger.error("Import batch id={} failed", request.getBatchId(), t);
        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
            eventPublisher.publishEvent(success ? AsyncRequestEvent.success(request, this)
                                                : AsyncRequestEvent.fail(request, this, failure));
            running.set(false);
        }
    }

    @Override
    public IAsyncRequest getRequest() { return request; }

    @Override
    public Long getBeginTime() { return beginTime; }

    @Override
    public Long getRunningTime() { return beginTime != null ? System.currentTimeMillis() - beginTime : null; }

    @Override
    public void terminate() {
        while (running.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public List<? extends IAsyncRequest> getRequests() {
        return Collections.singletonList(request);
    }
}
