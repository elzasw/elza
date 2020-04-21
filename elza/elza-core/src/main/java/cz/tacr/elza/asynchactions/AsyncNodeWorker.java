package cz.tacr.elza.asynchactions;

import com.google.common.eventbus.EventBus;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.events.ConformityInfoUpdatedEvent;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private Integer fundVersionId;

    private Long requestId;

    private Integer nodeId;

    public AsyncNodeWorker(Integer fundVersionId, Long requestId, Integer nodeId) {
        logger.debug("Vytvoren worker pro AS:" + fundVersionId + ", node: " + nodeId);
        this.fundVersionId = Validate.notNull(fundVersionId);
        this.beginTime = null;
        this.requestId = requestId;
        this.nodeId = nodeId;
    }

    @Override
    @Transactional
    public void run() {
        logger.debug("Start worker " + Thread.currentThread().getId() + " - " + System.currentTimeMillis());
        beginTime = System.currentTimeMillis();
        logger.debug("Spusteno AsyncNodeWorker ,  fundVersion : " + fundVersionId);

        Set<Integer> processedRequestIds = new LinkedHashSet<>();

        try {
            ArrFundVersion version = getFundVersion();

            if (ruleService.canUpdateConformity(requestId, nodeId)) {
                processRequest(requestId, nodeId, version);
                processedRequestIds.add(nodeId);
            }

            logger.debug("Run cyklus pro request : " + requestId + " - " + nodeId);
            eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.CONFORMITY_INFO, version,
            processedRequestIds.toArray(new Integer[processedRequestIds.size()])));
            logger.debug("Konec vlakna pro aktualizaci stavu, fundVersionId:" + fundVersionId);
            logger.debug("End worker " + Thread.currentThread().getId() + " - " + System.currentTimeMillis());
            eventPublisher.publishEvent(new AsyncRequestEvent(resultEvent()));

        } catch (Exception e) {
            logger.error("Unexpected error during conformity update", e);
        }
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    public Long getRequestId() {
        return requestId;
    }

    @Override
    public Integer getCurrentId() {
        return nodeId;
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

    private AsyncRequestVO resultEvent() {
        AsyncRequestVO publish = new AsyncRequestVO();
        publish.setType(AsyncTypeEnum.NODE);
        publish.setFundVersionId(fundVersionId);
        publish.setRequestId(requestId);
        publish.setNodeId(nodeId);
        return publish;
    }

    private ArrFundVersion getFundVersion() {
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
        if (version == null) {
            throw new EntityNotFoundException("ArrFundVersion for conformity update not found, versionId:" + fundVersionId);
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
            ArrNodeConformityExt arrNodeConformityExt = ruleService.setConformityInfo(levelId, fundVersionId, asyncRequestId);
            if (arrNodeConformityExt != null) {
                transactionManager.commit(transactionStatus);
            } else {
                transactionManager.rollback(transactionStatus);
            }
        } catch (Exception e) {
            logger.debug("Node chyba validace", e.getMessage());
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
        while (true) {
            try {
                Thread.sleep(100);

            } catch (InterruptedException e) {
                // Nothing to do with this -> simply finish
                Thread.currentThread().interrupt();
            }
        }
    }
}
