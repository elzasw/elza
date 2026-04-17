package cz.tacr.elza.service.cam.v2;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import cz.tacr.cam.v2.client.controller.vo.RequestProcessState;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.cam.v2.ItemSyncExportConfirmProcessor;
import cz.tacr.elza.cam.v2.ItemSyncExportProcessor;
import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * End-to-end test for the CAM v2 export flow (ELZA -> CAM).
 *
 * Uses WireMock to simulate the CAM server so the real {@code CamConnector}
 * and the generated REST client run — only the HTTP boundary is faked.
 *
 * Test data: reuses the access point loaded by the {@code SIMPLE-DEV} package
 * (UUID {@link #AP_UUID}). Every identifier created by this class (scope code,
 * system code, user name) is UUID-suffixed so the test doesn't collide with
 * leftover DB state, matching the long-term goal of cleanup-aware tests.
 */
public class CamServiceExportTest extends AbstractControllerTest {

    /** UUID of an AP preloaded by the SIMPLE-DEV package (see AccessPointControllerTest). */
    private static final String AP_UUID = "9f783015-b9af-42fc-bff4-11ff57cdb072";

    private static WireMockServer wireMockServer;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private AccessPointConnectorService accessPointConnectorService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    private CamV2MockHelper camMock;
    private String systemCode;
    // data created during the test, wiped in @AfterEach so subsequent tests' deleteTables
    // is not tripped up by orphan ExtSyncsQueueItem rows (HelperTestService.deleteTables
    // does not know about the queue table).
    private Integer queueItemIdToCleanup;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(options().port(0).notifier(new ConsoleNotifier(true)));
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @AfterEach
    void resetStubs() {
        if (camMock != null) {
            camMock.cleanUp();
        }
        wireMockServer.resetAll();
        if (queueItemIdToCleanup != null) {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                    extSyncsQueueItemRepository.deleteById(queueItemIdToCleanup));
            queueItemIdToCleanup = null;
        }
    }

    @Test
    void exportSuccess_createsBindingOnlyAfterConfirmation() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap, "SIMPLE-DEV package did not load the expected AP");

        ExtSyncsQueueItem queueItem = enqueueExportFor(ap.getAccessPointId(), externalSystemId);
        queueItemIdToCleanup = queueItem.getExtSyncsQueueItemId();

        // CAM response for POST /batches
        UUID batchUuid = UUID.randomUUID();
        camMock.stubPostBatch(batchUuid);

        // --- when: upload step --------------------------------------------
        ItemSyncProcessor uploadProcessor = accessPointConnectorService.nextItemSyncProcessor(1);
        assertTrue(uploadProcessor instanceof ItemSyncExportProcessor,
                   "Expected upload processor, got " + uploadProcessor);
        uploadProcessor.process();

        // --- then: queue advanced to PROCESSING, NO binding yet ------------
        ExtSyncsQueueItem afterUpload = reload(queueItem);
        assertEquals(ExtAsyncQueueState.EXPORT_PROCESSING, afterUpload.getState());
        assertEquals(batchUuid.toString(), afterUpload.getBatchId());
        assertNoBindingFor(ap.getAccessPointId(), externalSystemId);

        // --- given: CAM confirms the batch was stored ----------------------
        long assignedEntityId = 42_000L;
        String assignedEntityUuid = UUID.randomUUID().toString();
        String localId = "l1";
        String revisionUuid = UUID.randomUUID().toString();

        camMock.stubBatchStatus(batchUuid, RequestProcessState.FINISHED);
        camMock.stubBatchResultSuccess(batchUuid, assignedEntityId, assignedEntityUuid, localId, revisionUuid);

        // --- when: confirm step -------------------------------------------
        ItemSyncProcessor confirmProcessor = accessPointConnectorService.nextItemSyncProcessor(1);
        assertTrue(confirmProcessor instanceof ItemSyncExportConfirmProcessor,
                   "Expected confirm processor, got " + confirmProcessor);
        confirmProcessor.process();

        // --- then: queue EXPORT_OK and binding created with real entityId --
        ExtSyncsQueueItem afterConfirm = reload(queueItem);
        assertEquals(ExtAsyncQueueState.EXPORT_OK, afterConfirm.getState());

        ApExternalSystem extSystem = externalSystemService.findApExternalSystemByCode(systemCode);
        ApBindingState bindingState = bindingStateRepository
                .findByAccessPointAndExternalSystem(reloadAp(ap), extSystem);
        assertNotNull(bindingState, "Binding state should have been created on success");
        assertEquals(SyncState.SYNC_OK, bindingState.getSyncOk());
        assertEquals(revisionUuid, bindingState.getExtRevision());

        ApBinding binding = bindingState.getBinding();
        assertEquals(Long.toString(assignedEntityId), binding.getValue(),
                     "Binding value must be the real CAM entity id, not the temporary batch UUID");

        // items/parts bindings for this AP must exist
        List<ApBindingItem> bindingItems = bindingItemRepository.findByBinding(binding);
        assertTrue(bindingItems.size() > 0, "Expected per-part/per-item bindings to be persisted");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private int createExternalSystemForWireMock(String code) {
        ApScopeVO scope = createScope();
        ApExternalSystemVO vo = new ApExternalSystemVO();
        vo.setCode(code);
        vo.setName(code);
        // ApExternalSystem.url: CamInstance appends "/api/v2"
        vo.setUrl("http://localhost:" + wireMockServer.port());
        vo.setApiKeyId("test-key-id");
        vo.setApiKeyValue("test-key-value");
        vo.setType(ApExternalSystemType.CAM_V2);
        vo.setScopeId(scope.getId());
        SysExternalSystemVO created = createExternalSystem(vo);
        return created.getId();
    }

    private ExtSyncsQueueItem enqueueExportFor(Integer accessPointId, int externalSystemId) {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            ApState state = accessPointService.getApState(accessPointId);
            ApExternalSystem extSystem = externalSystemService.getExternalSystemInternal(externalSystemId);
            return externalSystemService.createExtSyncsQueueItem(
                    state.getAccessPoint(),
                    extSystem,
                    null,
                    null,
                    ExtAsyncQueueState.EXPORT_NEW,
                    OffsetDateTime.now(),
                    null /* user */);
        });
    }

    private ExtSyncsQueueItem reload(ExtSyncsQueueItem queueItem) {
        return extSyncsQueueItemRepository.findById(queueItem.getExtSyncsQueueItemId())
                .orElseThrow(() -> new AssertionError("Queue item disappeared"));
    }

    private ApAccessPoint reloadAp(ApAccessPoint ap) {
        return accessPointRepository.findById(ap.getAccessPointId())
                .orElseThrow(() -> new AssertionError("AP disappeared"));
    }

    private void assertNoBindingFor(Integer accessPointId, int externalSystemId) {
        ApAccessPoint ap = accessPointRepository.findById(accessPointId).orElseThrow();
        ApExternalSystem extSystem = externalSystemService.getExternalSystemInternal(externalSystemId);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(ap, extSystem);
        assertNull(bindingState,
                   "Binding must not exist before CAM confirms the batch — revoked entities would leave orphans otherwise");
    }
}
