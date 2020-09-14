package cz.tacr.elza.bulkaction;

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.service.AccessPointGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

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

    public AsyncAccessPointWorker(final AsyncRequest request) {
        this.request = request;
    }

    @Override
    public AsyncRequest getRequest() {
        return request;
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
