package cz.tacr.elza.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApFulltextProviderImpl;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Serviska pro úlohy, které je nutné spustit těsně po spuštění.
 */
@Service
public class StartupService implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(StartupService.class);

    private final NodeRepository nodeRepository;

    private final FundVersionRepository fundVersionRepository;

    private final UpdateConformityInfoService updateConformityInfoService;

    private final BulkActionRunRepository bulkActionRunRepository;

    private final OutputServiceInternal outputServiceInternal;

    private final RequestQueueService requestQueueService;

    private final NodeCacheService nodeCacheService;

    private final StaticDataService staticDataService;

    private final BulkActionConfigManager bulkActionConfigManager;

    private final EntityManager em;

    private final ApNameRepository apNameRepository;

    private final AccessPointService accessPointService;

    private final AccessPointGeneratorService accessPointGeneratorService;

    private boolean running;

    @Autowired
    public StartupService(NodeRepository nodeRepository,
                          FundVersionRepository fundVersionRepository,
                          UpdateConformityInfoService updateConformityInfoService,
                          BulkActionRunRepository bulkActionRunRepository,
                          OutputServiceInternal outputServiceInternal,
                          RequestQueueService requestQueueService,
                          NodeCacheService nodeCacheService,
                          StaticDataService staticDataService,
                          BulkActionConfigManager bulkActionConfigManager,
                          EntityManager em,
                          ApNameRepository apNameRepository,
                          final AccessPointService accessPointService,
                          final AccessPointGeneratorService accessPointGeneratorService) {
        this.nodeRepository = nodeRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.updateConformityInfoService = updateConformityInfoService;
        this.bulkActionRunRepository = bulkActionRunRepository;
        this.outputServiceInternal = outputServiceInternal;
        this.requestQueueService = requestQueueService;
        this.nodeCacheService = nodeCacheService;
        this.staticDataService = staticDataService;
        this.bulkActionConfigManager = bulkActionConfigManager;
        this.apNameRepository = apNameRepository;
        this.em = em;
        this.accessPointService = accessPointService;
        this.accessPointGeneratorService = accessPointGeneratorService;
    }

    @Autowired
    private StructObjValueService structureDataService;

    @Override
    @Transactional(value = TxType.REQUIRES_NEW)
    public void start() {
        logger.info("Elza startup service ...");

        ApFulltextProviderImpl fulltextProvider = new ApFulltextProviderImpl(apNameRepository);
        ArrDataRecordRef.setFulltextProvider(fulltextProvider);
        ArrDataPartyRef.setFulltextProvider(fulltextProvider);
        startInTransaction();

        running = true;
        logger.info("Elza startup finished");
    }

    @Override
    public void stop() {
        logger.info("Elza stopping ...");
        structureDataService.stopGenerator();
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
        DatabaseType.init(em);
        staticDataService.init();
        outputServiceInternal.init();
        clearBulkActions();
        clearTempStructureData();
        clearTempAccessPoint();
        bulkActionConfigManager.load();
        syncNodeCacheService();
        startNodeValidation();
        structureDataService.startGenerator();
        runQueuedRequests();
        runQueuedAccessPoints();
    }

    /**
     * Provede spuštění AP pro revalidaci.
     */
    private void runQueuedAccessPoints() {
        accessPointGeneratorService.restartQueuedAccessPoints();
    }

    /**
     * Provede vymazání nepoužitých dočasných AP všechně návazných dat.
     */
    private void clearTempAccessPoint() {
        accessPointService.removeTempAccessPoints();
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
            logger.warn("Detected unfinished actions, reseting to error state, count:" + affected);
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
                logger.error("Pro AF s ID=" + fundId + " byly nalezeny nezvalidované uzly (" + entry.getValue()
                + "), které nejsou z otevřené verze AF");
                continue;
            }

            // přidávání nodů je nutné dělat ve vlastní transakci (podle updateInfoForNodesAfterCommit)
            logger.info("Přidání " + entry.getValue().size() + " uzlů do fronty pro zvalidování");
            updateConformityInfoService.updateInfoForNodesAfterCommit(version.getFundVersionId(), entry.getValue());
        }
    }
}
