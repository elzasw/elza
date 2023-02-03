package cz.tacr.elza.asynchactions.nodevalid;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityNotFoundException;

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
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
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

                    new TransactionTemplate(transactionManager).execute((status) -> {
                        Set<Integer> processedRequestIds = new LinkedHashSet<>();
                        ArrFundVersion version = getFundVersion(request);

                        processRequest(requestId, nodeId, version);
                        processedRequestIds.add(nodeId);

                        eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.CONFORMITY_INFO, version, processedRequestIds.toArray(new Integer[0])));
                        return null;
                    });
                }
                eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
            }
        } catch (Throwable t) {
            logger.error("Validation failed", t);

            new TransactionTemplate(transactionManager).execute(status -> {
                handleException(t);
                return null;
            });
        } finally {
            long endTime = System.currentTimeMillis();
            logger.debug("End worker, threadId: {}, finished in {}ms",
                         Thread.currentThread().getId(),
                         endTime - beginTime);
            running.set(false);
        }
    }

    private void handleException(final Throwable t) {
        eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, t));
    }

    @Override
    public IAsyncRequest getRequest() {
        return request;
    }

    @Override
    public List<IAsyncRequest> getRequests() {
        return requests;
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
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
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
