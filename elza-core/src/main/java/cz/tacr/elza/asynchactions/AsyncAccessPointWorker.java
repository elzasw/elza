package cz.tacr.elza.asynchactions;

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
        logger.debug("Spusteno AsyncAccessPointWorker ,  accessPointId : " + accessPointId);
        try {
            new TransactionTemplate(transactionManager).execute((status) -> {
                accessPointGeneratorService.processRequest(accessPointId);
                eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
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

    private void handleException(final Throwable t) {
        eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, t));
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
                logger.info("Čekání na dokončení validace JP: {}", request.getNodeId());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Nothing to do with this -> simply finish
                Thread.currentThread().interrupt();
            }
        }
    }
}
