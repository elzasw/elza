package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityInfo;


/**
 * Servisní třída pro spouštění vláken na aktualizaci {@link ArrNodeConformityInfo}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Service
@Configuration
public class UpdateConformityInfoService {

    @Autowired
    @Qualifier(value = "conformityUpdateTaskExecutor")
    private Executor taskExecutor;


    /**
     * Mapa workerů pro dané verze.
     */
    private final Map<ArrFindingAidVersion, UpdateConformityInfoWorker> versionWorkers = new HashMap<>();


    /**
     * Provede spuštění vlákna pro výpočet nového stavu nad nody.
     *
     * @param updatedNodes seznam nodů k aktualizaci
     * @param version      verze, do které spadají nody
     */
    synchronized public void updateInfoForNodes(final Collection<ArrNode> updatedNodes,
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


    @Bean
    @Scope("prototype")
    public UpdateConformityInfoWorker createConformityInfoWorker(final Integer versionId) {
        return new UpdateConformityInfoWorker(versionId);
    }
}
