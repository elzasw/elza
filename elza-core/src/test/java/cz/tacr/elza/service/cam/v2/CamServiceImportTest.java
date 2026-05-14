package cz.tacr.elza.service.cam.v2;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
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
import cz.tacr.cam.v2.schema.cam.ItemStringXml;
import cz.tacr.cam.v2.schema.cam.ItemsXml;
import cz.tacr.cam.v2.schema.cam.LongStringXml;
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
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
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

        ExtSyncsQueueItem queueItem = enqueueImport(externalSystemId, camEntityId);

        // CAM returns the entity when the processor calls GET /entities/{id}
        EntityXml entityXml = buildEntity(camEntityId, camEntityUuid, "Imported person");
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

    private static EntityXml buildEntity(long entityId, String uuid, String prefName) {
        EntityXml ent = new EntityXml();
        ent.setEntityId(new EntityIdXml(entityId));
        ent.setEntityUuid(new UuidXml(uuid));
        ent.setState(EntityRecordStateXml.ERS_NEW);
        ent.setEntityType(new CodeXml("PERSON_BEING"));
        ent.setParts(new PartsXml());

        PartXml prefNamePart = new PartXml(new ItemsXml(), null, null, new UuidXml(uuid), PartTypeXml.PT_NAME);
        prefNamePart.getItems().getItems().add(
                new ItemStringXml(new StringXml(prefName), null, new CodeXml("NM_MAIN"), new UuidXml(uuid)));
        ent.getParts().getPart().add(prefNamePart);

        ent.setRevision(new RevisionInfoXml(new UuidXml(uuid), null, null,
                new UserInfoXml(new CodeXml("user"), null, new LongStringXml("user"), null, null, null),
                new DateTimeXml(OffsetDateTime.now()), null));
        return ent;
    }
}
