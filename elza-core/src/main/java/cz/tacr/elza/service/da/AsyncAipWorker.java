package cz.tacr.elza.service.da;

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
import cz.tacr.elza.service.UserService;

/**
 * Carries out one step of an action over AIPs - that is, one AIP.
 *
 * The AIP is the unit of work here for the same reason it is the unit of atomicity: it is the
 * granularity at which the action succeeds or fails, and finishing one AIP before starting the
 * next is what makes the progress of the action readable while it runs.
 */
@Component
@Scope("prototype")
public class AsyncAipWorker implements IAsyncWorker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAipWorker.class);

    @Autowired
    private UserService userService;

    @Autowired
    private DaAipStepService stepService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final AsyncRequest request;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Long beginTime;

    public AsyncAipWorker(final List<AsyncRequest> requests) {
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
        // Worker threads are pooled; the context of this user must not be left behind on the
        // thread for whatever runs on it next.
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        try {
            if (request.getUserId() != null) {
                SecurityContextHolder.setContext(userService.createSecurityContext(request.getUserId()));
            }
            stepService.runStep(request.getAipActionItemId());
            success = true;
        } catch (Throwable t) {
            failure = t;
            logger.error("Krok akce nad AIPem, položka ID={}, selhal", request.getAipActionItemId(), t);
            // The step records its own failure; anything reaching here happened outside it.
            stepService.recordUnexpectedFailure(request.getAipActionItemId(), t);
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
    public IAsyncRequest getRequest() {
        return request;
    }

    @Override
    public Long getBeginTime() {
        return beginTime;
    }

    @Override
    public Long getRunningTime() {
        return beginTime != null ? System.currentTimeMillis() - beginTime : null;
    }

    @Override
    public void terminate() {
        while (running.get()) {
            try {
                logger.info("Čekání na dokončení kroku akce nad AIPem: {}", request.getAipActionItemId());
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
