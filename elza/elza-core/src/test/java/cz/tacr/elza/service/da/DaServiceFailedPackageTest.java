package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;

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

    private ArrDigitalRepository repository;
    private DaSyncQueueItem syncQueueItem;

    @BeforeEach
    void setUp() {
        aipRepository = mock(AipRepository.class);
        aipStateRepository = mock(AipStateRepository.class);
        syncQueueItemRepository = mock(DaSyncQueueItemRepository.class);
        referenceResolver = mock(DaAipReferenceResolver.class);
        DaChangeRepository changeRepository = mock(DaChangeRepository.class);
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
        assertEquals("Balíček neobsahuje soubor PACKAGE-INFO.xml", syncQueueItem.getStateMessage());

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
        assertEquals("Balíček neobsahuje soubor PACKAGE-INFO.xml", problem.getValue().description());
        verify(aipStateRepository).save(aipState.getValue());
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
        verify(aipRepository, never()).findByCode(anyString());
        ArgumentCaptor<AipProblem> problem = ArgumentCaptor.forClass(AipProblem.class);
        verify(referenceResolver).recordProblem(any(DaAipState.class), problem.capture());
        assertEquals(AipProblemType.METADATA_ERROR, problem.getValue().type());
        verify(aipStateRepository).save(knownState);
    }

    private static InputStream zipOf(String entryName, String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return new ByteArrayInputStream(bytes.toByteArray());
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
