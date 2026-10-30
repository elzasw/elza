package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.api.AipProblemType;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * A package that arrives and cannot be processed must be as visible as one that could not be
 * downloaded: the failure is terminal - nothing retries it - and the queue is not where the
 * user looks for it, so the problem is recorded on the AIP, which is created when the DA
 * announced an AIP ELZA has never seen.
 */
public class DaServiceFailedPackageTest {

    private static final String CODE = "9c26d4bb-cb5b-4007-84df-121be156f722";

    private DaService service;

    private AipRepository aipRepository;
    private AipStateRepository aipStateRepository;
    private DaSyncQueueItemRepository syncQueueItemRepository;
    private DaAipReferenceResolver referenceResolver;
    private DaAipActionService actionService;
    private PackageInfoService packageInfoService;
    private DaLocalCacheRepository localCacheRepository;

    private ArrDigitalRepository repository;
    private DaSyncQueueItem syncQueueItem;

    @BeforeEach
    void setUp() {
        aipRepository = mock(AipRepository.class);
        aipStateRepository = mock(AipStateRepository.class);
        syncQueueItemRepository = mock(DaSyncQueueItemRepository.class);
        referenceResolver = mock(DaAipReferenceResolver.class);
        actionService = mock(DaAipActionService.class);
        DaChangeRepository changeRepository = mock(DaChangeRepository.class);
        packageInfoService = mock(PackageInfoService.class);
        localCacheRepository = mock(DaLocalCacheRepository.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(aipRepository.save(any(DaAip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aipStateRepository.save(any(DaAipState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(changeRepository.save(any(DaChange.class))).thenAnswer(inv -> inv.getArgument(0));

        repository = new ArrDigitalRepository();
        repository.setExternalSystemId(3);
        repository.setCode("DAMO");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);

        syncQueueItem = new DaSyncQueueItem();
        syncQueueItem.setCode(CODE);
        syncQueueItem.setAipVersion("2");
        syncQueueItem.setAipType(AipType.PACKAGE_INFO);
        syncQueueItem.setState(DaSyncQueueItem.QueueItemState.IMPORT_NEW);
        syncQueueItem.setDigitalRepository(repository);

        service = new DaService();
        setField(service, "aipRepository", aipRepository);
        setField(service, "aipStateRepository", aipStateRepository);
        setField(service, "syncQueueItemRepository", syncQueueItemRepository);
        setField(service, "changeRepository", changeRepository);
        setField(service, "referenceResolver", referenceResolver);
        setField(service, "actionService", actionService);
        setField(service, "packageInfoService", packageInfoService);
        setField(service, "daLocalCacheRepository", localCacheRepository);
        setField(service, "applicationContext", applicationContext);
        // The service reaches for itself through the context to open a transaction of its own;
        // the very same instance is what the transaction would run on.
        when(applicationContext.getBean(DaService.class)).thenReturn(service);
    }

    /**
     * What the DAMO development instance sends for dipType=package_info: the per-AIP directory
     * is named by the code of the AIP, but it holds a placeholder JSON instead of the
     * PACKAGE-INFO.xml the DA API prescribes.
     */
    @Test
    void packageWithoutPackageInfoIsRecordedOnANewlyCreatedAip() throws IOException {
        List<DaSyncQueueItem> batch = new ArrayList<>(List.of(syncQueueItem));

        service.processPackageInfo(repository, zipOf(CODE + "/package_info.json", "{\"aipId\":\"" + CODE + "\"}"),
                                   AipType.PACKAGE_INFO, batch);

        // Taken out of the batch, so the caller does not report it as loaded
        assertTrue(batch.isEmpty());
        assertEquals(DaSyncQueueItem.QueueItemState.IMPORT_ERROR, syncQueueItem.getState());
        // the message says what the package holds instead, which is the only account of it
        assertEquals("Balíček neobsahuje soubor PACKAGE-INFO.xml (obsahuje: package_info.json)",
                     syncQueueItem.getStateMessage());

        // The AIP the DA announced is created, so the failure is found in the AIP list
        ArgumentCaptor<DaAip> aip = ArgumentCaptor.forClass(DaAip.class);
        verify(aipRepository).save(aip.capture());
        assertEquals(CODE, aip.getValue().getCode());
        assertSame(repository, aip.getValue().getDigitalRepository());
        assertSame(aip.getValue(), syncQueueItem.getAip());

        ArgumentCaptor<DaAipState> aipState = ArgumentCaptor.forClass(DaAipState.class);
        ArgumentCaptor<AipProblem> problem = ArgumentCaptor.forClass(AipProblem.class);
        verify(referenceResolver).recordProblem(aipState.capture(), problem.capture());
        assertSame(aip.getValue(), aipState.getValue().getDaAip());
        assertEquals("2", aipState.getValue().getAipVersion());
        assertNotNull(aipState.getValue().getCreateChange());
        assertEquals(AipProblemType.METADATA_ERROR, problem.getValue().type());
        assertEquals("Balíček neobsahuje soubor PACKAGE-INFO.xml (obsahuje: package_info.json)",
                     problem.getValue().description());
        verify(aipStateRepository).save(aipState.getValue());

        // The item is out of the batch the caller closes, so its action item is finished here -
        // the request the user is watching would stay running otherwise
        verify(actionService).completeFromQueue(List.of(syncQueueItem), DaAipActionItemState.ERROR,
                                                "Balíček neobsahuje soubor PACKAGE-INFO.xml (obsahuje: package_info.json)");
    }

    /** An AIP ELZA already knows keeps its identity - the problem is written on its state. */
    @Test
    void packageOfAKnownAipIsRecordedOnItsExistingState() throws IOException {
        DaAip known = new DaAip();
        known.setAipId(11);
        known.setCode(CODE);
        known.setDigitalRepository(repository);
        DaAipState knownState = new DaAipState();
        knownState.setDaAip(known);
        knownState.setAipVersion("1");
        syncQueueItem.setAip(known);
        syncQueueItem.setState(DaSyncQueueItem.QueueItemState.UPDATE);
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(known)).thenReturn(knownState);

        service.processPackageInfo(repository, zipOf(CODE + "/package_info.json", "{}"),
                                   AipType.PACKAGE_INFO, new ArrayList<>(List.of(syncQueueItem)));

        verify(aipRepository, never()).save(any(DaAip.class));
        verify(aipRepository, never()).findByDigitalRepositoryAndCode(any(), anyString());
        ArgumentCaptor<AipProblem> problem = ArgumentCaptor.forClass(AipProblem.class);
        verify(referenceResolver).recordProblem(any(DaAipState.class), problem.capture());
        assertEquals(AipProblemType.METADATA_ERROR, problem.getValue().type());
        verify(aipStateRepository).save(knownState);
    }

    /**
     * A metadata request answered with a package that has the PACKAGE-INFO.xml but no METS.xml:
     * the AIP's state is read from it, but the package is not stored - storing it would flag
     * the metadata as loaded, and a later request for them would be refused as redundant.
     */
    @Test
    void metadataPackageWithoutMetsIsNotStored() throws Exception {
        DaAip known = new DaAip();
        known.setAipId(11);
        known.setCode(CODE);
        known.setDigitalRepository(repository);
        DaAipState knownState = new DaAipState();
        knownState.setDaAip(known);
        syncQueueItem.setAip(known);
        syncQueueItem.setAipType(AipType.METADATA_BASE);
        syncQueueItem.setState(DaSyncQueueItem.QueueItemState.UPDATE);
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(known)).thenReturn(knownState);
        when(packageInfoService.processPackageInfo(any(), any())).thenReturn(knownState);
        List<DaSyncQueueItem> batch = new ArrayList<>(List.of(syncQueueItem));

        service.processPackageInfo(repository, zipOf(CODE + "/PACKAGE-INFO.xml", "<premis/>"),
                                   AipType.METADATA_BASE, batch);

        assertTrue(batch.isEmpty());
        assertEquals(DaSyncQueueItem.QueueItemState.IMPORT_ERROR, syncQueueItem.getState());
        assertEquals("Balíček neobsahuje soubor METS.xml (obsahuje: PACKAGE-INFO.xml)", syncQueueItem.getStateMessage());
        verifyNoInteractions(localCacheRepository);
        assertNull(knownState.getMetadataLoad());
        assertNull(knownState.getCompleteAipLoad());
    }

    /**
     * A download that failed goes on in the queue, retried at growing intervals, but the request
     * the user is watching is finished with the failure now - a dialog that waits for the retries
     * would never end.
     */
    @Test
    void failedDownloadFinishesTheActionItemWhileTheQueueRetries() {
        ExternalSystemService externalSystemService = mock(ExternalSystemService.class);
        when(externalSystemService.getDigitalRepository(3)).thenReturn(repository);
        setField(service, "externalSystemService", externalSystemService);
        syncQueueItem.setState(DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED);

        service.recordDownloadFailure(List.of(syncQueueItem),
                                      new IllegalStateException("File Transfer nepřenesl žádný soubor dávky"));

        // the queue item waits for its retry
        assertEquals(DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED, syncQueueItem.getState());
        assertEquals(1, syncQueueItem.getAttemptCount());
        assertNotNull(syncQueueItem.getNextAttemptAt());
        assertTrue(syncQueueItem.getStateMessage().contains("File Transfer nepřenesl žádný soubor dávky"),
                   syncQueueItem.getStateMessage());

        // the action item is told now, with the note that the download goes on
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(actionService).completeFromQueue(eq(List.of(syncQueueItem)), eq(DaAipActionItemState.ERROR),
                                                message.capture());
        assertTrue(message.getValue().contains("Stažení bude opakováno"), message.getValue());
    }

    /**
     * The batch carries a package under another name than the code of the AIP asked for (a DA
     * naming the directory by the UUID of the package): the item must not pass as loaded.
     */
    @Test
    void itemWhosePackageIsNotInTheBatchFails() throws IOException {
        List<DaSyncQueueItem> batch = new ArrayList<>(List.of(syncQueueItem));

        service.processPackageInfo(repository, zipOf("dbdf00d0-85c4-4d86-8479-45f870fbb961/PACKAGE-INFO.xml", "<premis/>"),
                                   AipType.PACKAGE_INFO, batch);

        assertTrue(batch.isEmpty());
        assertEquals(DaSyncQueueItem.QueueItemState.IMPORT_ERROR, syncQueueItem.getState());
        assertEquals("Dávka neobsahuje balíček AIP " + CODE + " (dávka obsahuje: dbdf00d0-85c4-4d86-8479-45f870fbb961)",
                     syncQueueItem.getStateMessage());
        verifyNoInteractions(packageInfoService);
    }

    @Test
    void emptyBatchFailsItsItems() throws IOException {
        List<DaSyncQueueItem> batch = new ArrayList<>(List.of(syncQueueItem));

        service.processPackageInfo(repository, zipOf(java.util.Map.of()), AipType.PACKAGE_INFO, batch);

        assertTrue(batch.isEmpty());
        assertEquals("Dávka neobsahuje balíček AIP " + CODE + " (dávka je prázdná)", syncQueueItem.getStateMessage());
    }

    /**
     * What the ESM of Charles University delivers: the directory of the AIP holds one zip file,
     * and the package inside keeps its own root directory named by its UUID. Both wrappers are
     * removed, so the package is read, and stored, in the one layout.
     */
    @Test
    void packageDeliveredAsNestedZipIsUnwrapped() throws Exception {
        String uuid = "dbdf00d0-85c4-4d86-8479-45f870fbb961";
        byte[] inner = zipBytes(java.util.Map.of(
                uuid + "/metadata/preservation/PACKAGE-INFO.xml", "<premis/>".getBytes(StandardCharsets.UTF_8),
                uuid + "/METS.xml", "<mets/>".getBytes(StandardCharsets.UTF_8)));
        DaAipState state = new DaAipState();
        DaAip aip = new DaAip();
        aip.setCode(CODE);
        state.setDaAip(aip);
        ArgumentCaptor<java.io.File> packageInfo = ArgumentCaptor.forClass(java.io.File.class);
        when(packageInfoService.processPackageInfo(any(), packageInfo.capture())).thenReturn(state);
        List<DaSyncQueueItem> batch = new ArrayList<>(List.of(syncQueueItem));

        service.processPackageInfo(repository, zipOf(java.util.Map.of(CODE + "/" + uuid + ".zip", inner)),
                                   AipType.PACKAGE_INFO, batch);

        assertEquals(List.of(syncQueueItem), batch);
        assertEquals(DaSyncQueueItem.QueueItemState.IMPORT_NEW, syncQueueItem.getState());
        // read from the directory of the AIP, with no wrapper in between
        java.nio.file.Path read = packageInfo.getValue().toPath();
        assertEquals("PACKAGE-INFO.xml", read.getFileName().toString());
        assertEquals(CODE, read.getParent().getParent().getParent().getFileName().toString());
    }

    private static InputStream zipOf(String entryName, String content) throws IOException {
        return zipOf(java.util.Map.of(entryName, content.getBytes(StandardCharsets.UTF_8)));
    }

    private static InputStream zipOf(java.util.Map<String, byte[]> entries) throws IOException {
        return new ByteArrayInputStream(zipBytes(entries));
    }

    private static byte[] zipBytes(java.util.Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (java.util.Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
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
