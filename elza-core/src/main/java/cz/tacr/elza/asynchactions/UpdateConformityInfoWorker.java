package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.Transactional;

import cz.tacr.elza.exception.SystemException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Vlákno pro aktualizaci stavů nodů v jedné verzi.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Component
@Scope("prototype")
public class UpdateConformityInfoWorker implements Runnable {

    private Log logger = LogFactory.getLog(UpdateConformityInfoWorker.class);

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private EventNotificationService eventNotificationService;

    /**
     * Fronta nodů k aktualizaci stavu.
     */
    private Set<ArrNode> nodesToUpdate = new HashSet<>();

    private boolean running = true;
    private Integer versionId;

    /**
     * Zámek.
     */
    private Object lock = new Object();


    public UpdateConformityInfoWorker(final Integer versionId) {
        Assert.notNull(versionId);
        this.versionId = versionId;
    }

    @Override
    @Transactional
    public void run() {
        logger.info("Spusteno nove vlakno pro aktualizaci stavu ve verzi " + versionId);

        ArrFundVersion version = fundVersionRepository.findOne(versionId);

        Set<Integer> nodeIdsToFlush = new HashSet<>();

        try {
            while (true) {
                ArrNode node = null;
                synchronized (getLock()) {
                    if (nodesToUpdate.isEmpty()) {
                        running = false;
                        eventNotificationService.publishEvent(
                                EventFactory.createIdsInVersionEvent(EventType.CONFORMITY_INFO, version,
                                        nodeIdsToFlush.toArray(new Integer[nodeIdsToFlush.size()])));
                        break;
                    } else {
                        node = getNode();
                    }
                }

                //bylo by lepší přepsat metodu setConformityInfo, aby brala node
                ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootNode(),
                        version.getLockChange());
                try {
                    updateConformityInfoService.updateConformityInfo(node.getNodeId(), level.getLevelId(), versionId);
                    nodeIdsToFlush.add(node.getNodeId());
                } catch (LockVersionChangeException e) {
                    logger.info(
                            "Node " + node.getNodeId() + " nema aktualizovany stav. Behem validace ke zmene uzlu.");
                } catch (Exception e) {
                    logger.warn("Node " + node.getNodeId() + " nema aktualizovany stav. Behem validace došlo k chybě.",
                            e);
                }
            }
            logger.info("Konec vlakna pro aktualizaci stavu ve verzi" + versionId);
        } catch (Exception e) {
            logger.error(e);
        } finally {
            running = false;
        }
    }


    /**
     * Přidá nody do fronty.
     *
     * @param nodes seznam nodů k přidání
     */
    public void addNodes(Collection<ArrNode> nodes) {
        Assert.notNull(nodes);

        nodesToUpdate.addAll(nodes);
    }


    /**
     * Zjistí, jestli běží vlákno.
     *
     * @return true pokud běží, jinak false
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Vyjme jeden nod z fronty.
     *
     * @return nod z fronty
     */
    private ArrNode getNode() {
        ArrNode node = nodesToUpdate.iterator().next();
        nodesToUpdate.remove(node);
        return node;
    }

    /**
     * Vrací zámek.
     *
     * @return zámek
     */
    public Object getLock() {
        return lock;
    }

    /**
     * Provede ukončení běhu. Nechá dopočítat poslední uzel.
     */
    public void terminate() {
        synchronized (getLock()) {
            running = false;
            nodesToUpdate.clear();
        }
    }

    /**
     * Provede ukončení běhu. Počká než vlákno skutečně skončí.
     */
    public void terminateAndWait() {
        synchronized (getLock()) {
            nodesToUpdate.clear();
        }

        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new SystemException("Chyba při ukončování vlákna pro validaci uzlů.", e);
            }
        }
    }
}
