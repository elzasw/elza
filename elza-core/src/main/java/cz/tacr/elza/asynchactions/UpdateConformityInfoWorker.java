package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.events.ConformityInfoUpdatedEvent;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Vlákno pro aktualizaci stavů nodů v jedné verzi.
 */
@Component
@Scope("prototype")
public class UpdateConformityInfoWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(UpdateConformityInfoWorker.class);

    private static final int MAX_PROCESSED_NODES = 100;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Set<ArrNode> nodeQueue = new LinkedHashSet<>();

    private final Integer fundVersionId;

    private WorkerStatus status = WorkerStatus.RUNNABLE;

    private Thread thread;

    private enum WorkerStatus {
        RUNNABLE, RUNNING, TERMINATING, TERMINATED;

        public static boolean isRunning(WorkerStatus status) {
            return status == RUNNABLE || status == RUNNING;
        }
    }

    public UpdateConformityInfoWorker(Integer fundVersionId) {
        this.fundVersionId = Validate.notNull(fundVersionId);
    }

    @Override
    @Transactional
    public void run() {
        logger.debug("Spusteno nove vlakno pro aktualizaci stavu, fundVersionId: " + fundVersionId);

        synchronized (this) {
            Validate.isTrue(status == WorkerStatus.RUNNABLE);

            this.thread = Thread.currentThread();
            status = WorkerStatus.RUNNING;
        }

        Set<Integer> processedNodeIds = new LinkedHashSet<>();
        try {
            ArrFundVersion version = getFundVersion();
            long startTime = System.currentTimeMillis();
            while (true) {
                ArrNode node;
                synchronized (this) {
                    if (status != WorkerStatus.RUNNING) {
                        break;
                    }
                    node = getNextNode();
                }

                if (node == null || processedNodeIds.size() >= MAX_PROCESSED_NODES) {
                    logger.debug("Dokoncena revalidace uzlu, pocet: {}, cas: {} ms", processedNodeIds.size(), System
                            .currentTimeMillis() - startTime);
                    eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.CONFORMITY_INFO, version,
                            processedNodeIds.toArray(new Integer[processedNodeIds.size()])));
                    // terminate if last node
                    if (node == null) {
                        break;
                    }
                    startTime = System.currentTimeMillis();
                    processedNodeIds.clear();
                }
                processNode(node, version);
                processedNodeIds.add(node.getNodeId());
            }
            logger.debug("Konec vlakna pro aktualizaci stavu, fundVersionId:" + fundVersionId);
        } catch (Exception e) {
            logger.error("Unexpected error during conformity update", e);
        } finally {
            synchronized (this) {
                nodeQueue.clear();
                status = WorkerStatus.TERMINATED;
                thread = null;
            }
        }
    }

    private ArrFundVersion getFundVersion() {
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
        if (version == null) {
            throw new EntityNotFoundException("ArrFundVersion for conformity update not found, versionId:" + fundVersionId);
        }
        return version;
    }

    private void processNode(ArrNode node, ArrFundVersion version) {
        Integer nodeId = node.getNodeId();

        ArrLevel level = levelRepository.findByNode(node, version.getLockChange());

        if (level == null) {
            logger.info("Level does not exists in DB, nodeId = {}", node.getNodeId());
            return;
        }

        logger.debug("Aktualizace stavu " + nodeId + " ve verzi " + fundVersionId);

        try {
            updateConformityInfo(nodeId, level.getLevelId());

        } catch (LockVersionChangeException e) {
            logger.info("Node " + node.getNodeId() + " nema aktualizovany stav. Behem validace ke zmene uzlu.");
        } catch (Exception e) {
            logger.error("Node " + node.getNodeId() + " nema aktualizovany stav. Behem validace došlo k chybě.", e);
        }
    }

    private void updateConformityInfo(Integer nodeId, Integer levelId) {
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
            ruleService.setConformityInfo(levelId, fundVersionId);

            transactionManager.commit(transactionStatus);
        } catch (Exception e) {
            if (transactionStatus != null) {
                transactionManager.rollback(transactionStatus);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Přidá nody do fronty.
     *
     * @param nodes seznam nodů k přidání
     * @return False when cannot be added because worker is terminated.
     */
    public synchronized boolean addNodes(Collection<ArrNode> nodes) {
        Assert.notNull(nodes, "JP musí být vyplněny");
        if (!WorkerStatus.isRunning(status)) {
            return false;
        }
        nodeQueue.addAll(nodes);
        return true;
    }

    /**
     * Zjistí, jestli běží vlákno.
     *
     * @return true pokud běží, jinak false
     */
    public synchronized boolean isRunning() {
        return WorkerStatus.isRunning(status);
    }

    /**
     * Vyjme jeden nod z fronty. Pokud nemá další node ukončí běh vlákna.
     *
     * @return nod z fronty
     */
    private ArrNode getNextNode() {
        Iterator<ArrNode> it = nodeQueue.iterator();
        if (!it.hasNext()) {
            return null;
        }
        ArrNode node = it.next();
        it.remove();
        return node;
    }

    /**
     * Provede ukončení běhu. Počká než vlákno skutečně skončí.
     */
    public void terminateAndWait() {
        synchronized (this) {
            if (status == WorkerStatus.TERMINATED) {
                return;
            }
            status = WorkerStatus.TERMINATING;
        }
        while (true) {
            synchronized (this) {
                if (status == WorkerStatus.TERMINATED) {
                    break;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Nothing to do with this -> simply finish
                Thread.currentThread().interrupt();
            }
        }
    }

}
