package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.ElzaRules;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityInfo;
import cz.tacr.elza.events.ConformityInfoUpdatedEvent;


/**
 * Servisní třída pro spouštění vláken na aktualizaci {@link ArrNodeConformityInfo}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Service
@Configuration
public class UpdateConformityInfoService {

    private Log logger = LogFactory.getLog(UpdateConformityInfoWorker.class);

    @Autowired
    @Qualifier(value = "conformityUpdateTaskExecutor")
    private Executor taskExecutor;

    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ElzaRules elzaRules;

    private ThreadLocal<Set<ArrNode>> nodesToUpdate = new ThreadLocal<>();

    /**
     * Mapa workerů pro dané verze.
     */
    private final Map<ArrFindingAidVersion, UpdateConformityInfoWorker> versionWorkers = new HashMap<>();

    /**
     * Zapamatuje se uzly k přepočítání stavu a po dokončení transakce spustí jejich přepočet.
     *
     * @param updateNodes seznam nodů, které budou přepočítány.
     * @param version     verze, ve které probíhá výpočet
     */
    public void updateInfoForNodesAfterCommit(final Collection<ArrNode> updateNodes,
                                              final ArrFindingAidVersion version) {

        if (CollectionUtils.isEmpty(updateNodes)) {
            return;
        }

        if (CollectionUtils.isEmpty(nodesToUpdate.get())) {
            Set<ArrNode> nodes = new HashSet<>(updateNodes);
            nodesToUpdate.set(nodes);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    updateInfoForNodes(nodesToUpdate.get(), version);
                    nodesToUpdate.set(null);
                }
            });
        } else {
            nodesToUpdate.get().addAll(updateNodes);
        }
    }

    /**
     * Provede spuštění vlákna pro výpočet nového stavu nad nody.
     *
     * @param updatedNodes seznam nodů k aktualizaci
     * @param version      verze, do které spadají nody
     */
    synchronized private void updateInfoForNodes(final Collection<ArrNode> updatedNodes,
                                                final ArrFindingAidVersion version) {

        if (CollectionUtils.isEmpty(updatedNodes)) {
            return;
        }

        UpdateConformityInfoWorker updateConformityInfoWorker = versionWorkers.get(version);

        if (updateConformityInfoWorker == null) {
            startNewWorker(updatedNodes, version);
        } else {
            synchronized (updateConformityInfoWorker.getLock()) {
                if (updateConformityInfoWorker.isRunning()) {
                    updateConformityInfoWorker.addNodes(updatedNodes);
                } else {
                    startNewWorker(updatedNodes, version);
                }
            }
        }
    }

    /**
     * Registruje listener, který po úspěšném commitu transakce spustí výpočet stavu nodů.
     */
    private void registerAfterCommitListener() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {

            }
        });
    }

    /**
     * Provede ukončení běhu vlákna. (dopočítá poslední stav a ukončí se)
     */
    synchronized public void terminateWorkerInVersion(final ArrFindingAidVersion version) {
        UpdateConformityInfoWorker worker = versionWorkers.get(version);
        if (worker != null) {
            worker.terminate();
            versionWorkers.remove(worker);
        }
    }

    /**
     * Provede ukončení běhu vlákna.
     */
    synchronized public void terminateWorkerInVersionAndWait(final ArrFindingAidVersion version) {
        UpdateConformityInfoWorker worker = versionWorkers.get(version);
        if (worker != null && worker.isRunning()) {
            worker.terminateAndWait();
            versionWorkers.remove(worker);
        }
    }

    /**
     * Provede přidání nodů do fronty běžícího vlákna nebo založí nové.
     *
     * @param updatedNodes seznam nodů k aktualizaci
     * @param version      verze, do které nody spadají
     */
    synchronized private void startNewWorker(Collection<ArrNode> updatedNodes, ArrFindingAidVersion version) {
        Assert.notNull(version);

        UpdateConformityInfoWorker updateConformityInfoWorker = createConformityInfoWorker(
                version.getFindingAidVersionId());
        updateConformityInfoWorker.addNodes(updatedNodes);
        versionWorkers.put(version, updateConformityInfoWorker);

        taskExecutor.execute(updateConformityInfoWorker);
    }


    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void updateConformityInfo(final Integer nodeId, final Integer levelId, final Integer versionId) {
        logger.info("Aktualizace stavu " + nodeId + " ve verzi " + versionId);

        registerAfterCommitListener(nodeId);
        ruleManager.setConformityInfo(levelId, versionId, elzaRules.getStrategies(versionId));
    }

    /**
     * Registruje listener, který po úspěšném commitu transakce odešle aktualizované nody.
     */
    private void registerAfterCommitListener(final Integer nodeId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                eventBus.post(new ConformityInfoUpdatedEvent(nodeId));
            }
        });
    }


    @Bean
    @Scope("prototype")
    public UpdateConformityInfoWorker createConformityInfoWorker(final Integer versionId) {
        return new UpdateConformityInfoWorker(versionId);
    }
}
