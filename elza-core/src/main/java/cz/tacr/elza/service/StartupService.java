package cz.tacr.elza.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Serviska pro úlohy, které je nutné spustit těsně po spuštění.
 */
@Service
public class StartupService implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(StartupService.class);

    private boolean running;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private BulkActionRunRepository bulkActionRunRepository;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private RequestQueueService requestQueueService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private StructureDataService structureDataService;

    @Override
    @Transactional(value = TxType.REQUIRES_NEW)
    public void start() {
        running = true;
        startInTransaction();
    }

    @Override
    public void stop() {
        // TODO: stop async processes
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private void startInTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Active transaction required");
        }
        staticDataService.init();
        clearBulkActions();
        clearTempStructureData();
        clearOutputGeneration();
        syncNodeCacheService();
        startNodeValidation();
        runQueuedRequests();
    }

    /**
     * Provede vymazání nepoužitých dočasných hodnot strukt. typu.
     */
    private void clearTempStructureData() {
        structureDataService.removeTempStructureData();
    }

    /**
     * Provede spuštění synchronizace cache pro JP.
     */
    private void syncNodeCacheService() {
        nodeCacheService.syncCache();
    }

    /**
     * Provede spuštění neodeslaných požadavků ve frontě na externí systémy.
     */
    private void runQueuedRequests() {
        requestQueueService.restartQueuedRequests();
    }

    private void clearBulkActions() {
        int affected = bulkActionRunRepository.updateFromStateToState(ArrBulkActionRun.State.RUNNING, ArrBulkActionRun.State.ERROR);
        if (affected > 0) {
            LOG.warn("Detected unfinished actions, reseting to error state, count:" + affected);
        }
    }

    /**
     * Vyčistí hromadné akce - ty které jsou po startu ve stavu generování do chyba.
     */
    private void clearOutputGeneration() {
        int affected = outputDefinitionRepository.setStateFromStateWithError(Arrays.asList(OutputState.GENERATING, OutputState.COMPUTING),
                OutputState.OPEN, "Server se restartoval v průběhu zpracování");
        if (affected > 0) {
            LOG.warn("Bylo změněn stav " + affected + " outputů na stav Otevřený z důvodu restartování serveru při jejich zpracování.");
        }
    }

    /**
     * Provede přidání do front uzly, které nemají záznam v arr_node_conformity. Obvykle to jsou
     * uzly, které se validovaly během ukončení aplikačního serveru.
     * <p>
     * Metoda je pouštěna po startu aplikačního serveru.
     */
    @Transactional(value = TxType.MANDATORY)
    public void startNodeValidation() {
        // TransactionTemplate tmpl = new TransactionTemplate(txManager);
        Map<Integer, ArrFundVersion> fundVersionMap = new HashMap<>();
        Map<Integer, List<ArrNode>> fundNodesMap = new HashMap<>();

        // zjištění všech uzlů, které nemají validaci
        List<ArrNode> nodes = nodeRepository.findByNodeConformityIsNull();

        // roztřídění podle AF
        for (ArrNode node : nodes) {
            Integer fundId = node.getFund().getFundId();
            List<ArrNode> addedNodes = fundNodesMap.get(fundId);
            if (addedNodes == null) {
                addedNodes = new LinkedList<>();
                fundNodesMap.put(fundId, addedNodes);
            }
            addedNodes.add(node);
        }

        // načtení otevřených verzí AF
        List<ArrFundVersion> openVersions = fundVersionRepository.findAllOpenVersion();

        // vytvoření převodní mapy "id AF->verze AF"
        for (ArrFundVersion openVersion : openVersions) {
            fundVersionMap.put(openVersion.getFund().getFundId(), openVersion);
        }

        // projde všechny fondy
        for (Map.Entry<Integer, List<ArrNode>> entry : fundNodesMap.entrySet()) {
            Integer fundId = entry.getKey();
            ArrFundVersion version = fundVersionMap.get(fundId);

            if (version == null) {
                LOG.error("Pro AF s ID=" + fundId + " byly nalezeny nezvalidované uzly (" + entry.getValue()
                + "), které nejsou z otevřené verze AF");
                continue;
            }

            // přidávání nodů je nutné dělat ve vlastní transakci (podle updateInfoForNodesAfterCommit)
            LOG.info("Přidání " + entry.getValue().size() + " uzlů do fronty pro zvalidování");
            updateConformityInfoService.updateInfoForNodesAfterCommit(entry.getValue(), version);
        }
    }
}
