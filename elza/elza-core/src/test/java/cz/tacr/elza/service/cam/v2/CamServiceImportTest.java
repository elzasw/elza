package cz.tacr.elza.service.cam.v2;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
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

import cz.tacr.cam.v2.schema.cam.CodeXml;
import cz.tacr.cam.v2.schema.cam.DateTimeXml;
import cz.tacr.cam.v2.schema.cam.EntityIdXml;
import cz.tacr.cam.v2.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;
import cz.tacr.cam.v2.schema.cam.IssueSeverityXml;
import cz.tacr.cam.v2.schema.cam.IssuesXml;
import cz.tacr.cam.v2.schema.cam.ItemRefXml;
import cz.tacr.cam.v2.schema.cam.ItemStringXml;
import cz.tacr.cam.v2.schema.cam.ItemsXml;
import cz.tacr.cam.v2.schema.cam.LongStringXml;
import cz.tacr.cam.v2.schema.cam.PartRefXml;
import cz.tacr.cam.v2.schema.cam.PartTypeXml;
import cz.tacr.cam.v2.schema.cam.PartXml;
import cz.tacr.cam.v2.schema.cam.PartsXml;
import cz.tacr.cam.v2.schema.cam.RevisionInfoXml;
import cz.tacr.cam.v2.schema.cam.StringXml;
import cz.tacr.cam.v2.schema.cam.UserInfoXml;
import cz.tacr.cam.v2.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.cam.v2.ItemSyncImportProcessor;
import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ExtEntityBinding;
import cz.tacr.elza.controller.vo.ExtIssueIconState;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingIssue;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingIssueRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * End-to-end test for the CAM v2 import flow (CAM -> ELZA) driven by the
 * ext_syncs_queue + {@link ItemSyncImportProcessor}.
 *
 * Uses WireMock so the real {@code CamConnector} and generated REST client
 * run — only the HTTP boundary is faked. Complements
 * {@link CamServiceExportTest} which covers the reverse direction, and
 * {@link CamServiceTest#importNewTest} which only covers the lower-level
 * {@code synchronizeAccessPoint} call without the queue/processor layer.
 *
 * <h2>Why this test exists</h2>
 * Before this was added, the routing in {@code AccessPointConnectorService
 * .createDownloadProcessor} was hard-coded to the v1 processor, so importing
 * from a {@code CAM_COMPLETE_V2} (or any v2) external system failed with
 * {@code IllegalArgumentException: Externí systém není typu CAM} thrown by
 * {@code v1.CamConnector.get}. Any test that drives the queue-level import
 * through the processor would have caught it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CamServiceImportTest extends AbstractControllerTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private ApBindingIssueRepository bindingIssueRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApFactory apFactory;

    private CamV2MockHelper camMock;
    private String systemCode;
    /** Binding created by the current test — used by @AfterEach to clean queue items. */
    private Integer testBindingId;
    /** AP created by the import — tracked so @AfterEach can clean FK-dependent rows. */
    private String createdApUuid;

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
    }

    @AfterAll
    public void cleanupOnce() {
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
        // Queue items referencing the test binding / AP would otherwise trip the next
        // setUp (HelperTestService.deleteTables does not know about ext_syncs_queue_item).
        if (testBindingId != null || createdApUuid != null) {
            final Integer bindingId = testBindingId;
            final String apUuid = createdApUuid;
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                if (bindingId != null) {
                    bindingRepository.findById(bindingId)
                            .ifPresent(extSyncsQueueItemRepository::deleteByBinding);
                }
                if (apUuid != null) {
                    ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(apUuid);
                    if (ap != null) {
                        extSyncsQueueItemRepository.deleteByAccessPoint(ap);
                    }
                }
            });
            testBindingId = null;
            createdApUuid = null;
        }
    }

    /**
     * Happy path: a queue item in {@code IMPORT_NEW} pointing at a fresh binding
     * is picked up, the download processor fetches the entity via CAM v2, and
     * a new AP + bindingState is persisted.
     *
     * The downloaded entity carries two issues (a warning referencing a part and
     * an item by uuid, plus an error), so this also guards that issues are stored
     * on the initial download (not only on a later update), that their refs
     * resolve to local ids, that the client list is ordered errors-first, and
     * that the issue badge is computed. The resolved {@code partId}/{@code itemId}
     * matter: the parts/items and their {@code ApBindingItem}s are created in the
     * same transaction just before the issue resolver queries them, so a non-null
     * id proves those rows are visible to the resolver (i.e. flushed) at that point.
     *
     * Regression guard for the {@code CAM_COMPLETE_V2} routing bug: we
     * explicitly assert the returned processor is the v2 implementation.
     */
    @Test
    void importNew_createsAccessPointAndMarksQueueImportOk() {
        // --- given ---------------------------------------------------------
        camMock = new CamV2MockHelper(wireMockServer);
        systemCode = "CAM_V2_IMPORT_" + UUID.randomUUID();
        int externalSystemId = createExternalSystemForWireMock(systemCode);

        long camEntityId = 900_001L;
        String camEntityUuid = UUID.randomUUID().toString();
        createdApUuid = camEntityUuid;
        String partUuid = UUID.randomUUID().toString();
        String itemUuid = UUID.randomUUID().toString();
        String warningIssueUuid = UUID.randomUUID().toString();
        String errorIssueUuid = UUID.randomUUID().toString();

        ExtSyncsQueueItem queueItem = enqueueImport(externalSystemId, camEntityId);

        // CAM returns the entity (carrying a warning + an error) when the processor calls GET /entities/{id}
        EntityXml entityXml = buildEntity(camEntityId, camEntityUuid, "Imported person",
                partUuid, itemUuid, warningIssueUuid, errorIssueUuid);
        camMock.stubGetEntityById(camEntityId, entityXml);

        // --- when ---------------------------------------------------------
        ItemSyncProcessor processor = accessPointConnectorService.nextItemSyncProcessor(10);
        assertNotNull(processor, "Queue item in IMPORT_NEW should yield a download processor");
        assertTrue(processor instanceof ItemSyncImportProcessor,
                   "Download for CAM_V2 must be routed to the v2 ItemSyncImportProcessor, got " + processor);

        boolean ok = processor.process();
        assertTrue(ok, "process() should return true on a successful single-item import");

        // --- then ---------------------------------------------------------
        ExtSyncsQueueItem afterImport = reload(queueItem);
        assertEquals(ExtAsyncQueueState.IMPORT_OK, afterImport.getState(),
                     "Queue should transition to IMPORT_OK after a successful import");

        ApAccessPoint ap = accessPointRepository.findAccessPointByUuid(camEntityUuid);
        assertNotNull(ap, "A new AP should have been created for the imported entity");

        ApExternalSystem extSystem = externalSystemService.findApExternalSystemByCode(systemCode);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(ap, extSystem);
        assertNotNull(bindingState, "Binding state should link the new AP to the external system");
        assertEquals(SyncState.SYNC_OK, bindingState.getSyncOk());
        assertEquals(Long.toString(camEntityId), bindingState.getBinding().getValue());

        // issues are stored on download; the warning's part/item refs resolve to local ids
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            List<ApBindingIssue> issues = bindingIssueRepository.findByBindingId(testBindingId);
            assertEquals(2, issues.size(), "both of the entity's issues must be stored on download");

            ApBindingIssue warning = issues.stream()
                    .filter(i -> warningIssueUuid.equals(i.getUuid()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("warning issue not stored"));
            assertEquals(ApBindingIssue.Severity.WARNING, warning.getSeverity());
            assertEquals("Missing birth date", warning.getMessage());
            assertNotNull(warning.getPartId(),
                          "issue.partRef must resolve to a local partId — the part's ApBindingItem "
                                  + "must be visible to the resolver at issue-sync time");
            assertNotNull(warning.getItemId(),
                          "issue.itemRef must resolve to a local itemId");
        });

        // the client-facing list must be ordered: errors first, then stable by id
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            List<ApBindingIssue> ordered = bindingIssueRepository.findByBindingIdFetchRelated(testBindingId);
            assertEquals(2, ordered.size());
            assertEquals(ApBindingIssue.Severity.ERROR, ordered.get(0).getSeverity(),
                         "errors must be returned first regardless of insertion order");
            assertEquals(ApBindingIssue.Severity.WARNING, ordered.get(1).getSeverity());
        });

        // the issue badge must surface in the registry detail VO (the client's symptom:
        // issueSummary was null even though ap_binding_issue rows exist)
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            ApState apState = stateRepository.findLastByAccessPointId(ap.getAccessPointId());
            ApAccessPointVO vo = apFactory.createVO(apState, true);

            ExtEntityBinding bindingVo = vo.getBindings().stream()
                    .filter(b -> b.getId().equals(testBindingId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Binding VO missing for the imported AP"));

            assertNotNull(bindingVo.getIssueSummary(),
                          "issueSummary must not be null when the binding has issues");
            assertEquals(2, bindingVo.getIssueSummary().getCount());
            // an ERROR is present -> ATTENTION
            assertEquals(ExtIssueIconState.ATTENTION, bindingVo.getIssueSummary().getIconState());
        });
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private int createExternalSystemForWireMock(String code) {
        ApScopeVO scope = createScope();
        ApExternalSystemVO vo = new ApExternalSystemVO();
        vo.setCode(code);
        vo.setName(code);
        vo.setUrl("http://localhost:" + wireMockServer.port());
        vo.setApiKeyId("test-key-id");
        vo.setApiKeyValue("test-key-value");
        vo.setType(ApExternalSystemType.CAM_V2);
        vo.setScopeId(scope.getId());
        SysExternalSystemVO created = createExternalSystem(vo);
        return created.getId();
    }

    private ExtSyncsQueueItem enqueueImport(int externalSystemId, long camEntityId) {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            ApExternalSystem extSystem = externalSystemService.getExternalSystemInternal(externalSystemId);
            ApBinding binding = externalSystemService.createApBinding(Long.toString(camEntityId), extSystem, true);
            testBindingId = binding.getBindingId();
            return externalSystemService.createExtSyncsQueueItem(
                    null /* accessPoint — will be created on import */,
                    extSystem,
                    binding,
                    null,
                    ExtAsyncQueueState.IMPORT_NEW,
                    OffsetDateTime.now(),
                    null /* user — triggers createSecurityContextSystem in importEntities */);
        });
    }

    private ExtSyncsQueueItem reload(ExtSyncsQueueItem queueItem) {
        return extSyncsQueueItemRepository.findById(queueItem.getExtSyncsQueueItemId())
                .orElseThrow(() -> new AssertionError("Queue item disappeared"));
    }

    /**
     * Builds a minimal PERSON_BEING entity with one preferred-name part/item and
     * two issues: a WARNING (referencing that part and item by uuid) and, listed
     * after it, an ERROR. This exercises issue storage, ref resolution, the
     * errors-first ordering, and the ATTENTION badge aggregation.
     */
    private static EntityXml buildEntity(long entityId, String uuid, String prefName,
                                         String partUuid, String itemUuid,
                                         String warningIssueUuid, String errorIssueUuid) {
        EntityXml ent = new EntityXml();
        ent.setEntityId(new EntityIdXml(entityId));
        ent.setEntityUuid(new UuidXml(uuid));
        ent.setState(EntityRecordStateXml.ERS_NEW);
        ent.setEntityType(new CodeXml("PERSON_BEING"));
        ent.setParts(new PartsXml());

        PartXml prefNamePart = new PartXml(new ItemsXml(), null, null, new UuidXml(partUuid), PartTypeXml.PT_NAME);
        prefNamePart.getItems().getItems().add(
                new ItemStringXml(new StringXml(prefName), null, new CodeXml("NM_MAIN"), new UuidXml(itemUuid)));
        ent.getParts().getPart().add(prefNamePart);

        ent.setRevision(new RevisionInfoXml(new UuidXml(uuid), null, null,
                new UserInfoXml(new CodeXml("user"), null, new LongStringXml("user"), null, null, null),
                new DateTimeXml(OffsetDateTime.now()), null));

        ExistingIssueXml warning = new ExistingIssueXml();
        warning.setUuid(new UuidXml(warningIssueUuid));
        warning.setSeverity(IssueSeverityXml.WARNING);
        warning.setMessage(new StringXml("Missing birth date"));
        warning.setRuleCode(new CodeXml("RULE_X"));
        warning.setPartRef(new PartRefXml(new UuidXml(partUuid), PartTypeXml.PT_NAME));
        warning.setItemRef(new ItemRefXml(null, new CodeXml("NM_MAIN"), new UuidXml(itemUuid)));
        warning.setFrom(new DateTimeXml(OffsetDateTime.now()));

        // listed after the warning, so it gets a higher id — the errors-first
        // ordering must still place it before the warning.
        ExistingIssueXml error = new ExistingIssueXml();
        error.setUuid(new UuidXml(errorIssueUuid));
        error.setSeverity(IssueSeverityXml.ERROR);
        error.setMessage(new StringXml("Invalid data"));
        error.setRuleCode(new CodeXml("RULE_Y"));
        error.setFrom(new DateTimeXml(OffsetDateTime.now()));

        IssuesXml issues = new IssuesXml();
        issues.getIssue().add(warning);
        issues.getIssue().add(error);
        ent.setIssues(issues);
        return ent;
    }
}
