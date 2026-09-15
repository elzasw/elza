package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;

/**
 * The load flags of an AIP say what package is on disk. They are written where the package is
 * stored and nowhere else - a request that has not been served yet must not claim its package -
 * and a request that is already waiting in the queue is not made again.
 */
public class DaServiceLoadFlagsTest {

    private static final int AIP_ID = 11;

    private DaService service;

    private AipStateRepository aipStateRepository;
    private DaSyncQueueItemRepository syncQueueItemRepository;

    private DaAip aip;
    private DaAipState aipState;
    private ArrDigitalRepository repository;
    private RecordingSink sink;

    /** What the service reports about each AIP. */
    private static class RecordingSink implements AipOutcomeSink {
        final Map<Integer, String> skipped = new LinkedHashMap<>();
        final List<DaSyncQueueItem> enqueued = new ArrayList<>();

        @Override
        public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
            if (state == DaAipActionItemState.SKIPPED) {
                skipped.put(aipId, message);
            }
        }

        @Override
        public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
            enqueued.add(queueItem);
        }
    }

    @BeforeEach
    void setUp() {
        AipRepository aipRepository = mock(AipRepository.class);
        aipStateRepository = mock(AipStateRepository.class);
        syncQueueItemRepository = mock(DaSyncQueueItemRepository.class);
        DaLocalCacheRepository localCacheRepository = mock(DaLocalCacheRepository.class);
        DaAipActionService actionService = mock(DaAipActionService.class);
        DaAipReferenceResolver referenceResolver = mock(DaAipReferenceResolver.class);

        repository = new ArrDigitalRepository();
        repository.setExternalSystemId(3);
        repository.setCode("DA-REPO");

        aip = new DaAip();
        aip.setAipId(AIP_ID);
        aip.setCode("aip-code");
        aip.setDigitalRepository(repository);

        aipState = new DaAipState();
        aipState.setAipStateId(5);
        aipState.setDaAip(aip);
        aipState.setAipVersion("1");
        aipState.setFund(new ArrFund());

        sink = new RecordingSink();

        when(aipRepository.findAllById(anyCollection())).thenReturn(List.of(aip));
        when(aipStateRepository.findByDaAipInAndDeleteChangeIsNull(any())).thenReturn(List.of(aipState));
        when(aipStateRepository.save(any(DaAipState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncQueueItemRepository.save(any(DaSyncQueueItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actionService.start(any(), any())).thenReturn(new DaAipAction());
        when(actionService.sinkFor(any())).thenReturn(sink);

        service = new DaService();
        setField(service, "aipRepository", aipRepository);
        setField(service, "aipStateRepository", aipStateRepository);
        setField(service, "syncQueueItemRepository", syncQueueItemRepository);
        setField(service, "daLocalCacheRepository", localCacheRepository);
        setField(service, "actionService", actionService);
        setField(service, "referenceResolver", referenceResolver);

        // The service expects the caller to hold a transaction; here the test stands in for one.
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    /** A download of the given form is waiting in the queue for the AIP. */
    private void pendingDownload(AipType aipType) {
        DaSyncQueueItem pending = new DaSyncQueueItem();
        pending.setAipType(aipType);
        pending.setState(DaSyncQueueItem.QueueItemState.UPDATE);
        when(syncQueueItemRepository.findByAipAndStateInAndActiveIsTrue(eq(aip), any())).thenReturn(pending);
    }

    @Test
    void requestingMetadataDoesNotClaimThem() {
        service.createDaoStructure(List.of(AIP_ID), sink);

        assertEquals(1, sink.enqueued.size());
        assertEquals(AipType.METADATA_BASE, sink.enqueued.get(0).getAipType());
        assertNull(aipState.getMetadataLoad());
        assertNull(aipState.getCompleteAipLoad());
    }

    @Test
    void metadataOnDiskAreNotRequestedAgain() {
        aipState.setMetadataLoad(true);

        service.createDaoStructure(List.of(AIP_ID), sink);

        assertTrue(sink.enqueued.isEmpty());
        assertEquals("Metadata AIPu už jsou stažená.", sink.skipped.get(AIP_ID));
    }

    @Test
    void pendingMetadataDownloadIsNotRequestedAgain() {
        pendingDownload(AipType.METADATA_BASE);

        service.createDaoStructure(List.of(AIP_ID), sink);

        assertTrue(sink.enqueued.isEmpty());
        assertEquals("Stažení metadat AIPu už je ve frontě.", sink.skipped.get(AIP_ID));
    }

    @Test
    void pendingCompleteDownloadCoversAMetadataRequest() {
        pendingDownload(AipType.AIP_BASE);

        service.createDaoStructure(List.of(AIP_ID), sink);

        assertTrue(sink.enqueued.isEmpty());
    }

    @Test
    void pendingPackageInfoDoesNotCoverAMetadataRequest() {
        pendingDownload(AipType.PACKAGE_INFO);

        service.createDaoStructure(List.of(AIP_ID), sink);

        assertEquals(1, sink.enqueued.size());
    }

    @Test
    void storedMetadataPackageSetsTheFlags() {
        service.createImportLocalCache(aipState, repository, AipType.METADATA_BASE, Path.of("aip.zip"), new DaSyncQueueItem());

        assertTrue(aipState.getMetadataLoad());
        assertFalse(aipState.getCompleteAipLoad());
    }

    @Test
    void storedCompletePackageCarriesTheMetadataToo() {
        service.createImportLocalCache(aipState, repository, AipType.AIP_BASE, Path.of("aip.zip"), new DaSyncQueueItem());

        assertTrue(aipState.getMetadataLoad());
        assertTrue(aipState.getCompleteAipLoad());
    }

    @Test
    void metadataPackageReplacingTheCompleteOneClearsItsFlag() {
        aipState.setMetadataLoad(true);
        aipState.setCompleteAipLoad(true);

        service.createImportLocalCache(aipState, repository, AipType.METADATA_BASE, Path.of("aip.zip"), new DaSyncQueueItem());

        assertTrue(aipState.getMetadataLoad());
        assertFalse(aipState.getCompleteAipLoad());
    }

    @Test
    void requestingTheCompleteAipDoesNotClaimIt() {
        aipState.setMetadataLoad(true);

        service.aipDownloadCompleteAip(List.of(AIP_ID));

        assertEquals(1, sink.enqueued.size());
        assertEquals(AipType.AIP_BASE, sink.enqueued.get(0).getAipType());
        assertNull(aipState.getCompleteAipLoad());
    }

    @Test
    void deletingTheCompleteAipKeepsItsFlagUntilTheMetadataArrive() {
        aipState.setMetadataLoad(true);
        aipState.setCompleteAipLoad(true);

        service.aipDeleteCompleteAip(List.of(AIP_ID));

        assertEquals(1, sink.enqueued.size());
        assertEquals(AipType.METADATA_BASE, sink.enqueued.get(0).getAipType());
        assertTrue(aipState.getCompleteAipLoad());
    }

    @Test
    void pendingDowngradeIsNotRequestedAgain() {
        aipState.setMetadataLoad(true);
        aipState.setCompleteAipLoad(true);
        pendingDownload(AipType.METADATA_BASE);

        service.aipDeleteCompleteAip(List.of(AIP_ID));

        assertTrue(sink.enqueued.isEmpty());
        assertEquals("Nahrazení kompletního balíčku metadaty už je ve frontě.", sink.skipped.get(AIP_ID));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
