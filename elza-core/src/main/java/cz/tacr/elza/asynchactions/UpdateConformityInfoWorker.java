package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.api.vo.RuleEvaluationType;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.events.ConformityInfoUpdatedEvent;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;


/**
 * Vlákno pro aktualizaci stavů nodů v jedné verzi.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Component
@Scope("prototype")
public class UpdateConformityInfoWorker implements Runnable {

    private Log logger = LogFactory.getLog(UpdateConformityInfoWorker.class);

    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    /**
     * Fronta nodů k aktualizaci stavu.
     */
    private Set<ArrNode> nodesToUpdate = new HashSet<>();
    /**
     * Množina aktualizovaných nodů.
     */
    private Set<Integer> finishedNode = new HashSet<>();


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


        registerAfterCommitListener();
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);

        try {

            while (true) {
                ArrNode node = null;
                synchronized (getLock()) {
                    if (nodesToUpdate.isEmpty()) {
                        running = false;
                        break;
                    } else {
                        node = getNode();
                    }
                }

                logger.info("Aktualizace stavu " + node.getNodeId() + " ve verzi " + versionId);
                //bylo by lepší přepsat metodu setConformityInfo, aby brala node
                ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootLevel().getNode(),
                        version.getLockChange());
                ruleManager.setConformityInfo(level.getLevelId(), versionId, RuleEvaluationType.NORMAL);

                publishEventsAfterCommit(node.getNodeId());
            }
            logger.info("Konec vlakna pro aktualizaci stavu ve verzi\" + versionId");
        } catch (Exception e) {
            logger.error(e);
        } finally {
            running = false;
        }
    }

    /**
     * Přidá id nodu k aktualizaci.
     * @param nodeId id nodu
     */
    private void publishEventsAfterCommit(final Integer nodeId) {
        finishedNode.add(nodeId);
    }

    /**
     * Registruje listener, který po úspěšném commitu transakce odešle aktualizované nody.
     */
    private void registerAfterCommitListener() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                if (!finishedNode.isEmpty()) {
                    eventBus.post(new ConformityInfoUpdatedEvent(finishedNode));
                }
            }
        });
    }

    /**
     * Přidá nody do fronty.
     * @param nodes seznam nodů k přidání
     */
    public void addNodes(Collection<ArrNode> nodes) {
        Assert.notNull(nodes);

        nodesToUpdate.addAll(nodes);
    }


    /**
     * Zjistí, jestli běží vlákno.
     * @return true pokud běží, jinak false
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Vyjme jeden nod z fronty.
     * @return nod z fronty
     */
    private ArrNode getNode() {
        ArrNode node = nodesToUpdate.iterator().next();
        nodesToUpdate.remove(node);
        return node;
    }

    /**
     * Vrací zámek.
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
}
