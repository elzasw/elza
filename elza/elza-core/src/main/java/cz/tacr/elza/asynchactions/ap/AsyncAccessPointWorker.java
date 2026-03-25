package cz.tacr.elza.asynchactions.ap;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.service.AccessPointGeneratorService;

@Component
@Scope("prototype")
public class AsyncAccessPointWorker implements IAsyncWorker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAccessPointWorker.class);

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AccessPointGeneratorService accessPointGeneratorService;

    private final AsyncRequest request;
    private Long beginTime;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public AsyncAccessPointWorker(final List<AsyncRequest> requests) {
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
        Integer accessPointId = request.getAccessPointId();
        beginTime = System.currentTimeMillis();
        logger.debug("Spusteno AsyncAccessPointWorker, accessPointId: {}", accessPointId);
        boolean success = false;
        Throwable failure = null;
        try {
            new TransactionTemplate(transactionManager).execute((status) -> {
                accessPointGeneratorService.processRequest(accessPointId);
                return null;
            });
            success = true;
        } catch (Throwable t) {
            logger.error("Failed to process access point, id: {}, request: {}", accessPointId, request, t);
            failure = t;
        } finally {
            // Always notify executor so worker is removed from processing
            try {
                if (success) {
                    eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
                } else {
                    eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, failure));
                }
            } catch (Exception e) {
                logger.error("Failed to publish async request event", e);
            }
            running.set(false);
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

    @Override
    public void terminate() {
        while (running.get()) {
            try {
                logger.info("Čekání na dokončení validace AP: {}", request.getAccessPointId());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Nothing to do with this -> simply finish
                Thread.currentThread().interrupt();
            }
        }
    }
}
