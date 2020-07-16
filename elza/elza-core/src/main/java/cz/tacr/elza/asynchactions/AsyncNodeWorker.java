package cz.tacr.elza.asynchactions;

import com.google.common.eventbus.EventBus;
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

import javax.persistence.EntityNotFoundException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final AsyncRequest request;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public AsyncNodeWorker(final AsyncRequest request) {
        running.set(true);
        this.request = request;
    }

    @Override
    public void run() {
        Integer fundVersionId = request.getFundVersionId();
        Long requestId = request.getRequestId();
        Integer nodeId = request.getNodeId();

        logger.debug("Start worker " + Thread.currentThread().getId() + " - " + System.currentTimeMillis());
        beginTime = System.currentTimeMillis();
        logger.debug("Spusteno AsyncNodeWorker ,  fundVersion : " + fundVersionId);
        try {
            new TransactionTemplate(transactionManager).execute((status) -> {
                Set<Integer> processedRequestIds = new LinkedHashSet<>();
                ArrFundVersion version = getFundVersion();

                processRequest(requestId, nodeId, version);
                processedRequestIds.add(nodeId);

                logger.debug("Run cyklus pro request : " + requestId + " - " + nodeId);
                eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.CONFORMITY_INFO, version, processedRequestIds.toArray(new Integer[0])));
                logger.debug("Konec vlakna pro aktualizaci stavu, fundVersionId:" + fundVersionId);
                logger.debug("End worker " + Thread.currentThread().getId() + " - " + System.currentTimeMillis());
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
    public AsyncRequest getRequest() {
        return request;
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

    private ArrFundVersion getFundVersion() {
        ArrFundVersion version = fundVersionRepository.findOne(request.getFundVersionId());
        if (version == null) {
            throw new EntityNotFoundException("ArrFundVersion for conformity update not found, versionId: " + request.getFundVersionId());
        }
        return version;
    }

    private void processRequest(Long requestId, Integer nodeId, ArrFundVersion version) {

        ArrLevel level = levelRepository.findByNodeId(nodeId, version.getLockChange());

        if (level == null) {
            logger.debug("Level does not exists in DB, nodeId = {}", nodeId);
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

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    eventBus.post(new ConformityInfoUpdatedEvent(nodeId));
                }
            });
            ArrNodeConformityExt arrNodeConformityExt = ruleService.setConformityInfo(levelId, request.getFundVersionId(), asyncRequestId);
            if (arrNodeConformityExt != null) {
                transactionManager.commit(transactionStatus);
            } else {
                transactionManager.rollback(transactionStatus);
            }
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
                logger.info("Čekání na dokončení validace JP: {}", request.getNodeId());
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
        return request.equals(that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request);
    }
}
