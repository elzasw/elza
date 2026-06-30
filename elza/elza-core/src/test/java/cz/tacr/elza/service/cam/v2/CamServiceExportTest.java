package cz.tacr.elza.service.cam.v2;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.cam.v2.client.controller.vo.RequestProcessState;
import cz.tacr.cam.v2.schema.cam.PartTypeXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.cam.v2.ApIssue;
import cz.tacr.elza.cam.v2.ItemSyncExportConfirmProcessor;
import cz.tacr.elza.cam.v2.ItemSyncExportProcessor;
import cz.tacr.elza.cam.v2.UploadMapping;
import cz.tacr.elza.cam.v2.UuidMapping;
import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingParticipant;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingParticipantRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExtSyncsProcessor;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.cam.v2.CamV2MockHelper.IssueSpec;

/**
 * End-to-end tests for the CAM v2 export flow (ELZA -> CAM).
 *
 * Uses WireMock to simulate the CAM server so the real {@code CamConnector}
 * and the generated REST client run — only the HTTP boundary is faked.
 *
 * <h2>Scenarios covered</h2>
 * <ol>
 *   <li><b>Happy path</b> — entity uploaded, CAM accepts on first try.
 *       Queue transitions {@code EXPORT_NEW} → {@code EXPORT_PROCESSING} →
 *       {@code EXPORT_OK}. A binding is created with the real entity id only
 *       after CAM confirms storage.
 *       ({@link #exportSuccess_createsBindingOnlyAfterConfirmation})</li>
 *   <li><b>Warnings + user cancels</b> — CAM returns a warning-only failure
 *       with a {@code forceKey}. Queue moves to {@code EXPORT_NEED_CONFIRM}.
 *       User declines the force dialog → queue moves to
 *       {@code EXPORT_CANCELLED}. No binding is created and no
 *       {@code ACCESS_POINT_EXPORT_FAILED} event is published (the switch in
 *       {@code setQueueItemState} falls through for {@code EXPORT_CANCELLED}).
 *       ({@link #exportCancelled_terminatesInCancelledStateWithNoBinding})</li>
 *   <li><b>Warnings + user forces (after a previous cancel)</b> — user
 *       re-enqueues the AP. Same warnings come back. User clicks "Odeslat
 *       přesto". Queue item is re-uploaded with {@code force=true}; CAM
 *       accepts; binding created. Also asserts the POST carried the
 *       {@code force=true} query parameter.
 *       ({@link #exportWithForce_afterCancel_createsBinding})</li>
 *   <li><b>Issue-ref resolution</b> — on a warning that references a specific
 *       {@code partUuid}, the confirm processor resolves it back to the ELZA
 *       {@code partId} via the transient uuid map persisted on the queue item
 *       and enriches the serialized {@code ApIssue} with a human-readable
 *       {@code partName} pulled from the AP cache. Also verifies the uuid map
 *       is cleared when the queue reaches a terminal state.
 *       ({@link #warningWithPartRef_resolvesElzaPartIdFromUuidMap})</li>
 * </ol>
 *
 * <h2>Known gaps (follow-up tests)</h2>
 * <ul>
 *   <li>Hard {@code BatchChangeFailureXml} with {@code ERROR} severity →
 *       queue moves to {@code ERROR} and {@code ACCESS_POINT_EXPORT_FAILED}
 *       event fires.</li>
 *   <li>{@code getBatchStatus} returning {@code ERROR}.</li>
 *   <li>Request-body shape assertions (the batch XML actually sent to CAM).</li>
 * </ul>
 *
 * <h2>Data and cleanup</h2>
 * Tests reuse the access point preloaded by the {@code SIMPLE-DEV} package
 * (UUID {@link #AP_UUID}). Every per-test identifier (scope/system code) is
 * UUID-suffixed so the tests do not collide with leftover DB state. The
 * {@code @AfterEach} hook wipes queue items created against the test AP —
 * {@code HelperTestService.deleteTables} (called by the base class before
 * the next test) does not know about the queue table, so orphan rows would
 * otherwise trip the next test's setup with a FK violation.
 *
 * <p>Uses per-class lifecycle: base setUp/tearDown run once per class via
 * {@link #initOnce()} / {@link #cleanupOnce()}. Each test still uses a
 * UUID-suffixed {@code systemCode}, so external systems / bindings created by
 * earlier tests in the class cannot collide with later ones.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    private ApBindingParticipantRepository bindingParticipantRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private ApChangeRepository apChangeRepository;

    @Autowired
    private ApPartRepository apPartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExtSyncsProcessor extSyncsProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    private CamV2MockHelper camMock;
    private String systemCode;
    /** AP used by the current test — queue items referencing it are deleted in @AfterEach. */
    private Integer testApId;

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

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
        // The background ExtSyncsProcessor thread calls nextItemSyncProcessor on a timer
        // and would race with the synchronous nextItemSyncProcessor calls in these tests —
        // seen as OptimisticLockingFailureException on ExtSyncsQueueItem and queue items
        // unexpectedly flipping back out of EXPORT_PROCESSING. Pause it for the class.
        extSyncsProcessor.stopExtSyncs();
    }

    @AfterAll
    public void cleanupOnce() {
        extSyncsProcessor.startExtSyncs();
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    @AfterEach
    void resetStubs() {
        if (camMock != null) {
            camMock.cleanUp();
        }
        wireMockServer.resetAll();
        if (testApId != null) {
            // Delete all queue items referencing the test AP (may be several in Test B).
            // HelperTestService.deleteTables (called by AbstractTest.setUp) would otherwise
            // fail with a FK violation when it deletes ap_access_point rows.
            final Integer apId = testApId;
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                ApAccessPoint ap = accessPointRepository.findById(apId).orElse(null);
                if (ap != null) {
                    extSyncsQueueItemRepository.deleteByAccessPoint(ap);
                }
            });
            testApId = null;
        }
    }

    /**
     * Happy path: ELZA uploads an AP, CAM returns a successful
     * {@code BatchChangeSuccessXml}, binding is created with the real CAM
     * entity id. Regression guard: no binding exists before CAM confirms.
     */
    @Test
    void exportSuccess_createsBindingOnlyAfterConfirmation() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap, "SIMPLE-DEV package did not load the expected AP");
        testApId = ap.getAccessPointId();

        ExtSyncsQueueItem queueItem = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

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

    /**
     * CAM returns a warning-only failure. User declines to force the send
     * via the confirm dialog. The queue must terminate in
     * {@code EXPORT_CANCELLED} (not {@code ERROR}), no binding is created,
     * and no {@code ACCESS_POINT_EXPORT_FAILED} event is emitted — the
     * switch in {@code AccessPointConnectorService.setQueueItemState} has no
     * case for {@code EXPORT_CANCELLED}, so the state transition is silent
     * on the websocket.
     */
    @Test
    void exportCancelled_terminatesInCancelledStateWithNoBinding() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap);
        testApId = ap.getAccessPointId();

        ExtSyncsQueueItem queueItem = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

        UUID batchUuid = UUID.randomUUID();
        camMock.stubPostBatch(batchUuid);
        camMock.stubBatchStatus(batchUuid, RequestProcessState.FINISHED);
        String forceKey = UUID.randomUUID().toString();
        camMock.stubBatchResultFailure(batchUuid,
                Collections.singletonList(IssueSpec.warning(
                        "Hlavní část jména obsahuje pouze malá písmena.", "W_NAM_001")),
                forceKey);

        // --- when: upload + confirm ---------------------------------------
        accessPointConnectorService.nextItemSyncProcessor(1).process();  // upload
        accessPointConnectorService.nextItemSyncProcessor(1).process();  // confirm

        // --- then: queue waits for user decision, forceKey persisted ------
        ExtSyncsQueueItem afterConfirm = reload(queueItem);
        assertEquals(ExtAsyncQueueState.EXPORT_NEED_CONFIRM, afterConfirm.getState());
        assertEquals(forceKey, afterConfirm.getForceKey(),
                     "forceKey from the failure must be persisted on the queue item");
        assertNoBindingFor(ap.getAccessPointId(), externalSystemId);

        // --- when: user cancels --------------------------------------------
        accessPointConnectorService.exportForceOrNo(queueItem.getExtSyncsQueueItemId(), false);

        // --- then: terminal EXPORT_CANCELLED, still no binding -------------
        ExtSyncsQueueItem afterCancel = reload(queueItem);
        assertEquals(ExtAsyncQueueState.EXPORT_CANCELLED, afterCancel.getState(),
                     "Cancel must land in EXPORT_CANCELLED, not ERROR (ERROR would fire FAILED event)");
        assertNotNull(afterCancel.getStateMessage(),
                      "State message (warnings payload) is retained for audit");
        assertNoBindingFor(ap.getAccessPointId(), externalSystemId);
    }

    /**
     * Full force-after-cancel story: user cancels once, re-queues the AP,
     * receives the same warnings, and this time clicks "Odeslat přesto".
     * The AP is re-uploaded with {@code force=true}; CAM accepts; binding
     * created. Also verifies at the HTTP level that a POST with
     * {@code force=true} actually reached CAM.
     */
    @Test
    void exportWithForce_afterCancel_createsBinding() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap);
        testApId = ap.getAccessPointId();

        // --- phase 1: first queue item → warnings → user cancels -----------
        ExtSyncsQueueItem queueItem1 = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

        UUID batch1 = UUID.randomUUID();
        camMock.stubPostBatch(batch1);
        camMock.stubBatchStatus(batch1, RequestProcessState.FINISHED);
        String forceKey1 = UUID.randomUUID().toString();
        camMock.stubBatchResultFailure(batch1,
                Collections.singletonList(IssueSpec.warning("Warning #1", "W_NAM_001")),
                forceKey1);

        accessPointConnectorService.nextItemSyncProcessor(1).process();  // upload
        accessPointConnectorService.nextItemSyncProcessor(1).process();  // confirm → NEED_CONFIRM
        accessPointConnectorService.exportForceOrNo(queueItem1.getExtSyncsQueueItemId(), false);

        assertEquals(ExtAsyncQueueState.EXPORT_CANCELLED, reload(queueItem1).getState());
        assertNoBindingFor(ap.getAccessPointId(), externalSystemId);

        // --- phase 2: re-enqueue, same warnings come back ------------------
        ExtSyncsQueueItem queueItem2 = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

        UUID batch2 = UUID.randomUUID();
        // Newer stub takes priority over batch1's POST stub, so the second upload gets batch2.
        camMock.stubPostBatch(batch2);
        camMock.stubBatchStatus(batch2, RequestProcessState.FINISHED);
        String forceKey2 = UUID.randomUUID().toString();
        camMock.stubBatchResultFailure(batch2,
                Collections.singletonList(IssueSpec.warning("Warning #1", "W_NAM_001")),
                forceKey2);

        accessPointConnectorService.nextItemSyncProcessor(1).process();  // upload
        accessPointConnectorService.nextItemSyncProcessor(1).process();  // confirm → NEED_CONFIRM

        ExtSyncsQueueItem afterSecondConfirm = reload(queueItem2);
        assertEquals(ExtAsyncQueueState.EXPORT_NEED_CONFIRM, afterSecondConfirm.getState());
        assertEquals(forceKey2, afterSecondConfirm.getForceKey());

        // --- phase 3: user forces → re-POST with force=true, CAM accepts ---
        UUID batch3 = UUID.randomUUID();
        camMock.stubPostBatch(batch3);  // matches the force-true POST
        camMock.stubBatchStatus(batch3, RequestProcessState.FINISHED);

        long assignedEntityId = 77_777L;
        String revisionUuid = UUID.randomUUID().toString();
        camMock.stubBatchResultSuccess(batch3, assignedEntityId,
                UUID.randomUUID().toString(), "l1", revisionUuid);

        // `exportForceOrNo(..., true)` re-uploads synchronously via exportApForce,
        // so after this returns the queue is back in EXPORT_PROCESSING with batchId=batch3.
        accessPointConnectorService.exportForceOrNo(queueItem2.getExtSyncsQueueItemId(), true);
        assertEquals(ExtAsyncQueueState.EXPORT_PROCESSING, reload(queueItem2).getState());
        assertEquals(batch3.toString(), reload(queueItem2).getBatchId());

        // confirm step for the forced upload
        accessPointConnectorService.nextItemSyncProcessor(1).process();

        // --- then: binding created with the real entity id ----------------
        ExtSyncsQueueItem afterForceConfirm = reload(queueItem2);
        assertEquals(ExtAsyncQueueState.EXPORT_OK, afterForceConfirm.getState());

        ApExternalSystem extSystem = externalSystemService.findApExternalSystemByCode(systemCode);
        ApBindingState bindingState = bindingStateRepository
                .findByAccessPointAndExternalSystem(reloadAp(ap), extSystem);
        assertNotNull(bindingState, "Force send should have created the binding state");
        assertEquals(SyncState.SYNC_OK, bindingState.getSyncOk());
        assertEquals(Long.toString(assignedEntityId), bindingState.getBinding().getValue());

        // --- verify at the HTTP level: force=true actually hit CAM ---------
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/v2/batches"))
                .withQueryParam("force", equalTo("true")));
    }

    /**
     * A warning carrying a {@code partRef.partUuid} should produce an ApIssue
     * with a populated {@code partId}, resolved from the transient uuid map
     * persisted on the queue item at upload time. Also verifies that the
     * uuid map is cleared when the queue item reaches a terminal state
     * (here: {@code EXPORT_CANCELLED}).
     */
    @Test
    void warningWithPartRef_resolvesElzaPartIdFromUuidMap() throws Exception {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap);
        testApId = ap.getAccessPointId();

        ExtSyncsQueueItem queueItem = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

        UUID batchUuid = UUID.randomUUID();
        camMock.stubPostBatch(batchUuid);

        // --- when: upload → upload_map persisted on queue item --------------
        accessPointConnectorService.nextItemSyncProcessor(1).process();

        ExtSyncsQueueItem afterUpload = reload(queueItem);
        String uploadMapJson = afterUpload.getUploadMap();
        assertNotNull(uploadMapJson, "upload_map must be persisted at upload time");

        // Pick one part/uuid pair that the uploader generated — this is what CAM
        // would echo back in a warning's partRef. For this AP (SIMPLE-DEV) the
        // uploader creates new bindings for every part, so the map is non-empty.
        List<UuidMapping> entries = UploadMapping.deserialize(uploadMapJson).getUuidMappings();
        UuidMapping partEntry = entries.stream()
                .filter(e -> e.getPartId() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("upload_map has no part entries; cannot test partRef resolution"));

        // --- given: CAM returns a warning referencing that partUuid -------
        camMock.stubBatchStatus(batchUuid, RequestProcessState.FINISHED);
        camMock.stubBatchResultFailure(batchUuid,
                Collections.singletonList(
                        IssueSpec.warning("Warning referencing a specific part.", "W_NAM_001")
                                .withPart(partEntry.getUuid(), PartTypeXml.PT_NAME)),
                UUID.randomUUID().toString());

        // --- when: confirm step parses failure and runs resolver ----------
        accessPointConnectorService.nextItemSyncProcessor(1).process();

        // --- then: stateMessage JSON carries the resolved partId -----------
        ExtSyncsQueueItem afterConfirm = reload(queueItem);
        assertEquals(ExtAsyncQueueState.EXPORT_NEED_CONFIRM, afterConfirm.getState());

        List<ApIssue> issues = objectMapper.readValue(afterConfirm.getStateMessage(),
                new TypeReference<List<ApIssue>>() {});
        assertEquals(1, issues.size());
        ApIssue resolvedIssue = issues.get(0);
        assertEquals(partEntry.getPartId(), resolvedIssue.getPartId(),
                "Resolver should have mapped the CAM partUuid back to the ELZA partId via the upload_map");
        assertNull(resolvedIssue.getItemId(), "No itemRef on this warning");
        assertNull(resolvedIssue.getEntityId(), "No entityRef on this warning");
        // Phase 2a — name resolution via CachedAccessPoint; at minimum the part type
        // description (fallback) is always present, so a non-null name proves the
        // resolver looked up the AP cache for the referenced part.
        assertNotNull(resolvedIssue.getPartName(),
                "partName should be resolved from the AP cache (DISPLAY_NAME or part-type description)");

        // upload_map is still present in NEED_CONFIRM — needed for a subsequent force re-send
        assertNotNull(afterConfirm.getUploadMap(), "upload_map must survive the NEED_CONFIRM transition");

        // --- when: user cancels → queue transitions to terminal state ------
        accessPointConnectorService.exportForceOrNo(queueItem.getExtSyncsQueueItemId(), false);

        // --- then: upload_map is cleared on EXPORT_CANCELLED -----------------
        ExtSyncsQueueItem afterCancel = reload(queueItem);
        assertEquals(ExtAsyncQueueState.EXPORT_CANCELLED, afterCancel.getState());
        assertNull(afterCancel.getUploadMap(),
                "upload_map must be cleared when the queue item reaches a terminal state");
    }

    /**
     * Participants on the AP are reported as {@code addParticipant} change
     * actions. The test seeds an {@link ApPart#getLastChange()} for the test
     * AP that points at a brand-new {@link ApChange} attributed to an active
     * {@link UsrUser}. The collector must detect that user as an EDITOR, and
     * the uploaded batch XML must carry an {@code <addParticipant>} entry
     * with role EDITOR and the seeded user's id.
     */
    @Test
    void participantActivity_editorIsEmittedFromApChangeHistory() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap, "SIMPLE-DEV package did not load the expected AP");
        testApId = ap.getAccessPointId();

        // Seed: an active user recorded as the last editor of one of the AP's parts.
        // Reusing the test AP as the user's person AP keeps fixtures minimal — the AP
        // has no binding on the fresh external system, so the registry's entityId
        // path stays null and we exercise the id + uuid route.
        Integer editorUserId = new TransactionTemplate(transactionManager).execute(tx -> {
            ApAccessPoint reloaded = accessPointRepository.findById(testApId).orElseThrow();

            UsrUser editor = new UsrUser();
            editor.setUsername("participant-editor-" + UUID.randomUUID());
            editor.setActive(true);
            editor.setAccessPoint(reloaded);
            editor = userRepository.save(editor);

            ApChange editChange = new ApChange();
            editChange.setChangeDate(OffsetDateTime.now());
            editChange.setUser(editor);
            editChange.setType(ApChange.Type.AP_UPDATE);
            editChange = apChangeRepository.save(editChange);

            List<ApPart> parts = apPartRepository.findValidPartByAccessPoint(reloaded);
            assertFalse(parts.isEmpty(), "Test AP should have at least one part");
            ApPart part = parts.get(0);
            part.setLastChange(editChange);
            apPartRepository.save(part);

            return editor.getUserId();
        });
        assertNotNull(editorUserId);

        ExtSyncsQueueItem queueItem = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

        UUID batchUuid = UUID.randomUUID();
        camMock.stubPostBatch(batchUuid);

        // --- when: upload step --------------------------------------------
        accessPointConnectorService.nextItemSyncProcessor(1).process();

        assertEquals(ExtAsyncQueueState.EXPORT_PROCESSING, reload(queueItem).getState());

        // --- then: the posted batch XML carries an addParticipant for the editor
        List<LoggedRequest> posts = wireMockServer.findAll(postRequestedFor(
                urlPathEqualTo("/api/v2/batches")));
        assertEquals(1, posts.size(), "Expected exactly one POST to /batches on upload");
        String body = posts.get(0).getBodyAsString();

        assertTrue(body.contains("<addParticipant>"),
                   "Batch XML should carry at least one <addParticipant> action; body was:\n" + body);
        assertTrue(body.contains("<role>EDITOR</role>"),
                   "At least one participant must carry role EDITOR; body was:\n" + body);
        assertTrue(body.contains("<id>" + editorUserId + "</id>"),
                   "The seeded editor's userId should appear as a UserInfoXml id; body was:\n" + body);
    }

    /**
     * The participants sent to CAM in the batch must be mirrored locally into
     * {@code ap_binding_participant} once CAM confirms storage, tied to the new
     * binding state revision. Seeds an editor exactly as
     * {@link #participantActivity_editorIsEmittedFromApChangeHistory} does, then
     * drives the full upload + confirm flow and asserts the row is persisted as
     * an AUTHOR with the user-info name that went on the wire.
     */
    @Test
    void exportSuccess_persistsApBindingParticipants() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_EXPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(AP_UUID);
        assertNotNull(ap, "SIMPLE-DEV package did not load the expected AP");
        testApId = ap.getAccessPointId();

        // Seed an active user recorded as the last editor of one of the AP's parts.
        String editorUsername = "participant-editor-" + UUID.randomUUID();
        Integer editorUserId = new TransactionTemplate(transactionManager).execute(tx -> {
            ApAccessPoint reloaded = accessPointRepository.findById(testApId).orElseThrow();

            UsrUser editor = new UsrUser();
            editor.setUsername(editorUsername);
            editor.setActive(true);
            editor.setAccessPoint(reloaded);
            editor = userRepository.save(editor);

            ApChange editChange = new ApChange();
            editChange.setChangeDate(OffsetDateTime.now());
            editChange.setUser(editor);
            editChange.setType(ApChange.Type.AP_UPDATE);
            editChange = apChangeRepository.save(editChange);

            List<ApPart> parts = apPartRepository.findValidPartByAccessPoint(reloaded);
            assertFalse(parts.isEmpty(), "Test AP should have at least one part");
            ApPart part = parts.get(0);
            part.setLastChange(editChange);
            apPartRepository.save(part);

            return editor.getUserId();
        });
        assertNotNull(editorUserId);

        ExtSyncsQueueItem queueItem = enqueueExportFor(ap.getAccessPointId(), externalSystemId);

        UUID batchUuid = UUID.randomUUID();
        camMock.stubPostBatch(batchUuid);

        // --- when: upload step --------------------------------------------
        accessPointConnectorService.nextItemSyncProcessor(1).process();
        assertEquals(ExtAsyncQueueState.EXPORT_PROCESSING, reload(queueItem).getState());

        // --- given: CAM confirms the batch was stored ----------------------
        long assignedEntityId = 55_000L;
        String revisionUuid = UUID.randomUUID().toString();
        camMock.stubBatchStatus(batchUuid, RequestProcessState.FINISHED);
        camMock.stubBatchResultSuccess(batchUuid, assignedEntityId,
                UUID.randomUUID().toString(), "l1", revisionUuid);

        // --- when: confirm step -------------------------------------------
        accessPointConnectorService.nextItemSyncProcessor(1).process();

        // --- then: binding state created and the editor mirrored locally ---
        assertEquals(ExtAsyncQueueState.EXPORT_OK, reload(queueItem).getState());

        ApExternalSystem extSystem = externalSystemService.findApExternalSystemByCode(systemCode);
        ApBindingState bindingState = bindingStateRepository
                .findByAccessPointAndExternalSystem(reloadAp(ap), extSystem);
        assertNotNull(bindingState, "Binding state should have been created on success");

        List<ApBindingParticipant> participants = bindingParticipantRepository
                .findByBindingStateIdInOrderByLastChange(List.of(bindingState.getBindingStateId()));
        // Find the seeded editor specifically — the AP may also carry approver
        // participants from its own history, so do not assert an exact count.
        ApBindingParticipant editorParticipant = participants.stream()
                .filter(p -> editorUsername.equals(p.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Seeded editor was not mirrored into ap_binding_participant; participants: " + participants));

        assertEquals(ApBindingParticipant.Role.AUTHOR, editorParticipant.getRole(),
                     "A CAM EDITOR participant must be stored locally as AUTHOR");
        assertEquals(bindingState.getBindingStateId(), editorParticipant.getBindingStateId(),
                     "Participant must be tied to the new binding state revision");
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
