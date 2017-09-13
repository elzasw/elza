package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Vlákno pro aktualizaci stavů nodů v jedné verzi.
 */
@Component
@Scope("prototype")
public class UpdateConformityInfoWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateConformityInfoWorker.class);

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private EntityManager em;

    /**
     * Fronta nodů k aktualizaci stavu.
     */
    private final Set<ArrNode> updateNodeQueue = new LinkedHashSet<>();

    private final int versionId;

    private WorkerStatus status = WorkerStatus.RUNNABLE;

    public UpdateConformityInfoWorker(int versionId) {
        this.versionId = versionId;
    }

    @Override
    @Transactional
    public void run() {
        LOG.debug("Spusteno nove vlakno pro aktualizaci stavu, fundVersionId: " + versionId);

        Set<Integer> processedNodeIds = new LinkedHashSet<>();
        status = WorkerStatus.RUNNING;
        try {
            ArrFundVersion version = getFundVersion();
            while (true) {
                ArrNode node = getNextNode();
                if (node == null) {
                    eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.CONFORMITY_INFO, version,
                            processedNodeIds.toArray(new Integer[processedNodeIds.size()])));
                    break;
                }
                processNode(node, version);
                processedNodeIds.add(node.getNodeId());
            }
            LOG.debug("Konec vlakna pro aktualizaci stavu, fundVersionId:" + versionId);
        } catch (Exception e) {
            LOG.error("Unexpected error during conformity update", e);
        } finally {
            terminate();
        }
    }

    private ArrFundVersion getFundVersion() {
        ArrFundVersion version = fundVersionRepository.findOne(versionId);
        if (version == null) {
            throw new EntityNotFoundException("ArrFundVersion for conformity update not found, versionId:" + versionId);
        }
        return version;
    }

    private void processNode(ArrNode node, ArrFundVersion version) {
        // bylo by lepší přepsat metodu setConformityInfo, aby brala node
        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootNode(), version.getLockChange());
        try {
            updateConformityInfoService.updateConformityInfo(node.getNodeId(), level.getLevelId(), versionId);
        } catch (LockVersionChangeException e) {
            LOG.info("Node " + node.getNodeId() + " nema aktualizovany stav. Behem validace ke zmene uzlu.");
        } catch (Exception e) {
            LOG.error("Node " + node.getNodeId() + " nema aktualizovany stav. Behem validace došlo k chybě.", e);
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
        updateNodeQueue.addAll(nodes);
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
    private synchronized ArrNode getNextNode() {
        Iterator<ArrNode> it = updateNodeQueue.iterator();
        if (!it.hasNext()) {
            terminate();
            return null;
        }
        ArrNode node = it.next();
        it.remove();
        return node;
    }

    /**
     * Provede ukončení běhu. Nechá dopočítat poslední uzel.
     */
    public synchronized void terminate() {
        updateNodeQueue.clear();
        status = WorkerStatus.TERMINATED;
    }

    /**
     * Provede ukončení běhu. Počká než vlákno skutečně skončí.
     */
    public void terminateAndWait() {
        synchronized (this) {
            updateNodeQueue.clear();
            status = WorkerStatus.TERMINATING;
        }
        while (status != WorkerStatus.TERMINATED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new SystemException("Chyba při ukončování vlákna pro validaci uzlů.", e);
            }
        }
    }

    private enum WorkerStatus {
        RUNNABLE, RUNNING, TERMINATING, TERMINATED;

        public static boolean isRunning(WorkerStatus status) {
            return status == RUNNABLE || status == RUNNING;
        }
    }
}
