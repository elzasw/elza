package cz.tacr.elza.service;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApFulltextProviderImpl;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.VisiblePolicyRepository;
import cz.tacr.elza.search.DbQueueProcessor;
import cz.tacr.elza.search.IndexWorkProcessor;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cam.CamScheduler;

/**
 * Serviska pro úlohy, které je nutné spustit těsně po spuštění.
 */
@Service
public class StartupService implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(StartupService.class);

    private final ArrangementService arrangementService;

    private final NodeRepository nodeRepository;

    private final VisiblePolicyRepository visiblePolicyRepository;

    private final NodeConformityErrorRepository nodeConformityErrorRepository;

    private final NodeConformityMissingRepository nodeConformityMissingRepository;

    private final NodeConformityRepository nodeConformityRepository;

    private final BulkActionRunRepository bulkActionRunRepository;

    private final OutputServiceInternal outputServiceInternal;

    private final RequestQueueService requestQueueService;

    private final NodeCacheService nodeCacheService;

    private final StaticDataService staticDataService;

    private final BulkActionConfigManager bulkActionConfigManager;

    private final EntityManager em;

    private final AccessPointService accessPointService;

    private final IndexWorkProcessor indexWorkProcessor;

    private final ApplicationContext applicationContext;

    private final AsyncRequestService asyncRequestService;

    private final ExtSyncsProcessor extSyncsProcessor;

    private final AccessPointCacheService accessPointCacheService;

    private final CamScheduler camScheduler; 

    private boolean running;

    public static boolean fullTextReindex = false;

    /**
     * Service should start automatically by default
     * 
     * It is possible to disable autoStart, used by tests
     */
    @Value("${elza.startupService.autoStart:true}")
    private boolean autoStart = true;

    @Autowired
    AdminService adminService;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    public StartupService(final NodeRepository nodeRepository,
                          final ArrangementService arrangementService,
                          final BulkActionRunRepository bulkActionRunRepository,
                          final OutputServiceInternal outputServiceInternal,
                          final RequestQueueService requestQueueService,
                          final NodeCacheService nodeCacheService,
                          final StaticDataService staticDataService,
                          final BulkActionConfigManager bulkActionConfigManager,
                          final EntityManager em,
                          final AccessPointService accessPointService,
                          final NodeConformityErrorRepository nodeConformityErrorRepository,
                          final NodeConformityMissingRepository nodeConformityMissingRepository,
                          final NodeConformityRepository nodeConformityRepository,
                          final VisiblePolicyRepository visiblePolicyRepository,
                          IndexWorkProcessor indexWorkProcessor,
                          final ApplicationContext applicationContext,
                          final AsyncRequestService asyncRequestService,
                          final ExtSyncsProcessor extSyncsProcessor,
                          final AccessPointCacheService accessPointCacheService,
                          final CamScheduler camScheduler) {
        this.nodeRepository = nodeRepository;
        this.arrangementService = arrangementService;
        this.bulkActionRunRepository = bulkActionRunRepository;
        this.outputServiceInternal = outputServiceInternal;
        this.requestQueueService = requestQueueService;
        this.nodeCacheService = nodeCacheService;
        this.staticDataService = staticDataService;
        this.bulkActionConfigManager = bulkActionConfigManager;
        this.em = em;
        this.accessPointService = accessPointService;
        this.nodeConformityErrorRepository = nodeConformityErrorRepository;
        this.nodeConformityMissingRepository = nodeConformityMissingRepository;
        this.nodeConformityRepository = nodeConformityRepository;
        this.visiblePolicyRepository = visiblePolicyRepository;
        this.indexWorkProcessor = indexWorkProcessor;
        this.applicationContext = applicationContext;
        this.asyncRequestService = asyncRequestService;
        this.extSyncsProcessor = extSyncsProcessor;
        this.accessPointCacheService = accessPointCacheService;
        this.camScheduler = camScheduler;
    }

    @Autowired
    private StructObjValueService structureDataService;

    /**
     * Default service start method
     */
    @Override
    public void start() {
        if (!autoStart) {
            logger.info("Elza startup service - autoStart is disabled");
            return;
        }
        startNow();
    }

    /**
     * Method for explicit starting of the service
     */
    public void startNow() {
        Validate.isTrue(!running, "Already started");

        long startTime = System.currentTimeMillis();
        logger.info("Elza startup service ...");

        ApFulltextProviderImpl fulltextProvider = new ApFulltextProviderImpl(accessPointService);
        ArrDataRecordRef.setFulltextProvider(fulltextProvider);
        ApCachedAccessPointClassBridge.init(applicationContext.getBean(SettingsService.class));

        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(r -> startInTransaction());

        syncApCacheService();

        if (fullTextReindex) {
            logger.info("Full text reindex ...");
            tt.executeWithoutResult(r -> adminService.reindexInternal());
        }

        tt.executeWithoutResult(r -> startInTransaction2());

        camScheduler.start();

        running = true;
        logger.info("Elza startup finished in {} ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void stop() {
        logger.info("Elza stopping ...");
        camScheduler.stop();
        asyncRequestService.stop();
        indexWorkProcessor.stopIndexing();
        structureDataService.stopGenerator();
        outputServiceInternal.stop();
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
        DbQueueProcessor.startInit(applicationContext);
        outputServiceInternal.init();
        clearBulkActions();
        clearTempStructureData();
        clearOrphanedNodes();
        bulkActionConfigManager.load();
        syncNodeCacheService();
        // kontrola datové struktury
        accessPointService.checkConsistency();
    }

    private void startInTransaction2() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Active transaction required");
        }
        structureDataService.startGenerator();
        indexWorkProcessor.startIndexing();
        extSyncsProcessor.startExtSyncs();

        // je třeba volat mimo současnou transakci
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                asyncRequestService.start();
                arrangementService.startNodeValidation();
            }
        });

        runQueuedRequests();
    }

    private void clearOrphanedNodes() {
        logger.debug("Finding orpahed nodes ...");
        List<Integer> unusedNodes = nodeRepository.findUnusedNodeIds();
        if (CollectionUtils.isEmpty(unusedNodes)) {
            logger.debug("Orpahed nodes not found. It is OK");
            return;
        }
        // log findings
        logger.info("Found orpahed nodes, count = {}", unusedNodes.size());

        // try to fix issue by dropping these nodes
        visiblePolicyRepository.deleteByNodeIdIn(unusedNodes);
        nodeConformityErrorRepository.deleteByNodeConformityNodeIdIn(unusedNodes);
        nodeConformityMissingRepository.deleteByNodeConformityNodeIdIn(unusedNodes);
        nodeConformityRepository.deleteByNodeIdIn(unusedNodes);
        nodeCacheService.deleteNodes(unusedNodes);
        nodeRepository.deleteByNodeIdIn(unusedNodes);
        logger.info("Orpahed nodes deleted.");
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
     * Provede spuštění synchronizace cache pro AP.
     */
    private void syncApCacheService() {
        accessPointCacheService.syncCacheParallel();
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
}
