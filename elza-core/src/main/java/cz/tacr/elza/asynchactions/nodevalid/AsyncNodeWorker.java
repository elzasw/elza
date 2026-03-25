package cz.tacr.elza.asynchactions.nodevalid;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.EntityNotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncRequest;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.events.ConformityInfoUpdatedEvent;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;

@Component
@Scope("prototype")
public class AsyncNodeWorker implements IAsyncWorker {
    private static final Logger logger = LoggerFactory.getLogger(AsyncNodeWorker.class);

    private Long beginTime;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EventBus eventBus;

    private IAsyncRequest request;

    private final List<IAsyncRequest> requests;

    /**
     * Requests that were attempted (successfully processed or failed).
     * Unprocessed requests remain in DB for later pickup.
     */
    private final List<IAsyncRequest> processedRequests = new ArrayList<>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    public AsyncNodeWorker(final List<IAsyncRequest> requests) {
        running.set(true);
        this.requests = requests;
        if (CollectionUtils.isNotEmpty(requests)) {
            this.request = requests.get(0);
        } else {
            this.request = null;
        }
    }

    @Override
    public void run() {
        beginTime = System.currentTimeMillis();
        logger.debug("Start worker, threadId: {},  beginAt: {}",
                     Thread.currentThread().getId(), beginTime);
        Throwable failure = null;
        try {
            if (CollectionUtils.isNotEmpty(requests)) {
                for (IAsyncRequest request : requests) {
                    Integer fundVersionId = request.getFundVersionId();
                    Long requestId = request.getRequestId();
                    Integer nodeId = request.getCurrentId();
                    this.request = request;

                    long nodeBeginTime = System.currentTimeMillis();
                    logger.debug("Start worker, threadId: {},  beginAt: {}, fundVersionId: {}, nodeId: {}",
                            Thread.currentThread().getId(), nodeBeginTime,
                            fundVersionId, nodeId);

                    processedRequests.add(request);
                    new TransactionTemplate(transactionManager).execute((status) -> {
                        ArrFundVersion version = getFundVersion(request);

                        processRequest(requestId, nodeId, version);

                        eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(
                                EventType.CONFORMITY_INFO, version, nodeId));
                        return null;
                    });
                }
            }
        } catch (Throwable t) {
            logger.error("Validation failed for nodeId: {}", request.getCurrentId(), t);
            failure = t;
        } finally {
            // Always notify executor so worker is removed from processing
            try {
                if (failure != null) {
                    eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, failure));
                } else {
                    eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
                }
            } catch (Exception e) {
                logger.error("Failed to publish async request event", e);
            }
            long endTime = System.currentTimeMillis();
            logger.debug("End worker, threadId: {}, finished in {}ms",
                         Thread.currentThread().getId(),
                         endTime - beginTime);
            running.set(false);
        }
    }

    @Override
    public IAsyncRequest getRequest() {
        return request;
    }

    /**
     * Returns only processed requests (successfully completed or failed).
     * Unprocessed requests remain in DB for later pickup by queue restore.
     */
    @Override
    public List<IAsyncRequest> getRequests() {
        return processedRequests;
    }

    public Integer getFundVersionId() {
        return request.getFundVersionId();
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

    private ArrFundVersion getFundVersion(IAsyncRequest request) {
        return fundVersionRepository.findById(request.getFundVersionId())
                .orElseThrow(() -> new EntityNotFoundException("ArrFundVersion for conformity update not found, versionId: " + request.getFundVersionId()));
    }

    private void processRequest(Long requestId, Integer nodeId, ArrFundVersion version) {

        ArrLevel level = levelRepository.findByNodeId(nodeId, version.getLockChange());

        if (level == null) {
            logger.debug("Valid level for nodeId={}, versionId={} does not exists in DB",
                         nodeId, version.getFundVersionId());
            // we can drop previous state
            // TODO: refactor and use method for specific fundVersion
            ruleService.deleteByNodeIdIn(Collections.singletonList(nodeId));
            return;
        }

        logger.debug("Aktualizace stavu " + nodeId + " request " + requestId);

        try {
            updateConformityInfo(nodeId, level.getLevelId(), requestId);
            logger.debug("updateConformityInfo " + nodeId + " , request " + requestId);
        } catch (LockVersionChangeException e) {
            logger.debug("Node " + nodeId + " nema aktualizovany stav. Behem validace došlo ke zmene uzlu.");
        } catch (Exception e) {
            logger.debug("Node " + nodeId + " nema aktualizovany stav. Behem validace byl zvalidován v jiným requestem: " + requestId);
        }
    }

    private void updateConformityInfo(Integer nodeId, Integer levelId, Long asyncRequestId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus transactionStatus = null;
        try {
            transactionStatus = transactionManager.getTransaction(def);

            ArrNodeConformityExt arrNodeConformityExt = ruleService.setConformityInfo(levelId, request.getFundVersionId(), asyncRequestId);
            if (arrNodeConformityExt != null) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventBus.post(new ConformityInfoUpdatedEvent(nodeId));
                    }
                });
            }
            transactionManager.commit(transactionStatus);
        } catch (Exception e) {
            logger.debug("Node chyba validace", e);
            if (transactionStatus != null) {
                transactionManager.rollback(transactionStatus);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Provede ukončení běhu. Počká než vlákno skutečně skončí.
     */
    public void terminate() {
        while (running.get()) {
            try {
                logger.info("Čekání na dokončení validace JP: {}", request.getCurrentId());
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
        AsyncNodeWorker that = (AsyncNodeWorker) o;
        return requests.equals(that.requests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requests);
    }
}
