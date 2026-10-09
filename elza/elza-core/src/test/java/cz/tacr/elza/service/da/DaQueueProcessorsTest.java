package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cz.tacr.da.ApiException;
import cz.tacr.da.controller.vo.ErrorInfo;
import cz.tacr.da.controller.vo.IngestIngestResult;
import cz.tacr.da.controller.vo.IngestPackageIngestError;
import cz.tacr.da.controller.vo.IngestPackageIngestSuccess;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.domain.DaSyncQueueItem.QueueItemState;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * Each step of the processors is one exchange with the DA: a batch in flight is asked about
 * once and rescheduled, never waited for, and only the DA ends it.
 */
public class DaQueueProcessorsTest {

    private DaService daService;
    private DaAipActionService actionService;
    private ArrDigitalRepository repository;
    private DaImportExtSyncsProcessor importProcessor;
    private DaExportExtSyncsProcessor exportProcessor;

    @BeforeEach
    void setUp() {
        daService = mock(DaService.class);
        actionService = mock(DaAipActionService.class);
        ExternalSystemService externalSystemService = mock(ExternalSystemService.class);
        repository = new ArrDigitalRepository();
        repository.setExternalSystemId(5);
        when(externalSystemService.getDigitalRepository(5)).thenReturn(repository);

        importProcessor = new DaImportExtSyncsProcessor();
        setField(importProcessor, "daService", daService);
        setField(importProcessor, "externalSystemService", externalSystemService);
        setField(importProcessor, "actionService", actionService);

        exportProcessor = new DaExportExtSyncsProcessor();
        setField(exportProcessor, "daService", daService);
        setField(exportProcessor, "externalSystemService", externalSystemService);
        setField(exportProcessor, "actionService", actionService);
    }

    private DaSyncQueueItem item(String code, QueueItemState state, String batchId) {
        DaSyncQueueItem item = new DaSyncQueueItem();
        item.setCode(code);
        item.setState(state);
        item.setBatchId(batchId);
        item.setAipType(AipType.METADATA_BASE);
        item.setDigitalRepository(repository);
        return item;
    }

    private static Exception daFailure(int code) {
        return new IllegalStateException("Došlo k chybě při volání DA", new ApiException("HTTP " + code, code, Map.of(), null));
    }

    // --- downloads

    @Test
    void aRequestedBatchIsMarkedAndNotWaitedFor() {
        List<DaSyncQueueItem> items = List.of(item("a", QueueItemState.UPDATE, null));
        when(daService.getNextItems(anyInt(), eq(QueueItemState.DOWNLOAD_REQUESTED), any(), any())).thenReturn(items);
        when(daService.downloadAips(repository, items, AipType.METADATA_BASE)).thenReturn("d1");

        assertTrue(importProcessor.requestNextBatch());

        verify(daService).markBatch(items, QueueItemState.DOWNLOAD_REQUESTED, "d1", repository);
        verify(daService, never()).downloadStatusFinished(any(), anyString());
    }

    @Test
    void aBatchStillPreparedIsAskedAboutAgainLater() {
        List<DaSyncQueueItem> batch = List.of(item("a", QueueItemState.DOWNLOAD_REQUESTED, "d1"));
        when(daService.getDueBatch(QueueItemState.DOWNLOAD_REQUESTED)).thenReturn(batch);
        when(daService.downloadStatusFinished(repository, "d1")).thenReturn(false);

        assertFalse(importProcessor.checkRequestedBatch());

        verify(daService).scheduleNextCheck(batch, repository);
        verify(daService, never()).recordDownloadFailure(any(), any());
    }

    @Test
    void aBatchTheDaGaveUpIsRequestedAgain() {
        List<DaSyncQueueItem> batch = List.of(item("a", QueueItemState.DOWNLOAD_REQUESTED, "d1"));
        when(daService.getDueBatch(QueueItemState.DOWNLOAD_REQUESTED)).thenReturn(batch);
        Exception gone = daFailure(404);
        when(daService.downloadStatusFinished(repository, "d1")).thenThrow((RuntimeException) gone);

        assertFalse(importProcessor.checkRequestedBatch());

        verify(daService).returnToPending(batch, gone);
    }

    @Test
    void aStatusTheDaDidNotAnswerIsAskedAgainAfterADelay() {
        List<DaSyncQueueItem> batch = List.of(item("a", QueueItemState.DOWNLOAD_REQUESTED, "d1"));
        when(daService.getDueBatch(QueueItemState.DOWNLOAD_REQUESTED)).thenReturn(batch);
        Exception down = daFailure(503);
        when(daService.downloadStatusFinished(repository, "d1")).thenThrow((RuntimeException) down);

        assertFalse(importProcessor.checkRequestedBatch());

        verify(daService).recordDownloadFailure(batch, down);
        verify(daService, never()).returnToPending(any(), any());
    }

    // --- exports

    @Test
    void aSentBatchStillIngestedIsAskedAboutAgainLater() {
        List<DaSyncQueueItem> batch = List.of(item("a", QueueItemState.EXPORT_SENT, "e1"));
        when(daService.getDueBatch(QueueItemState.EXPORT_SENT)).thenReturn(batch);
        when(daService.ingestStatusFinished(repository, "e1")).thenReturn(false);

        assertFalse(exportProcessor.checkSentBatch());

        verify(daService).scheduleNextCheck(batch, repository);
        verify(daService, never()).changeQueueItemsState(any(), any(), any());
    }

    @Test
    void eachPackageEndsByWhatTheDaSaidAboutIt() {
        DaSyncQueueItem accepted = item("a", QueueItemState.EXPORT_SENT, "e1");
        DaSyncQueueItem refused = item("b", QueueItemState.EXPORT_SENT, "e1");
        DaSyncQueueItem unmentioned = item("c", QueueItemState.EXPORT_SENT, "e1");
        when(daService.getDueBatch(QueueItemState.EXPORT_SENT)).thenReturn(List.of(accepted, refused, unmentioned));
        when(daService.ingestStatusFinished(repository, "e1")).thenReturn(true);
        IngestIngestResult result = new IngestIngestResult();
        IngestPackageIngestSuccess success = new IngestPackageIngestSuccess();
        success.setSipId("a");
        success.setAipId("a");
        success.setAipVersion("3");
        result.addAcceptedItem(success);
        IngestPackageIngestError error = new IngestPackageIngestError();
        error.setSipId("b");
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorTitle("Neplatný EAD");
        errorInfo.setErrorDetail("chybí archdesc");
        error.setErrorInfo(errorInfo);
        result.addPackageIngestErrorItem(error);
        when(daService.ingestResult(repository, "e1")).thenReturn(result);

        assertTrue(exportProcessor.checkSentBatch());

        verify(daService).changeQueueItemsState(List.of(accepted), QueueItemState.EXPORT_OK, "Přijato jako verze 3.");
        verify(daService).changeQueueItemsState(List.of(refused), QueueItemState.EXPORT_ERROR,
                                                "Digitální archiv balíček odmítl: Neplatný EAD: chybí archdesc");
        verify(daService).changeQueueItemsState(List.of(unmentioned), QueueItemState.EXPORT_ERROR,
                                                DaExportExtSyncsProcessor.NOT_CONFIRMED);
        verify(actionService).completeFromQueue(List.of(accepted), DaAipActionItemState.FINISHED, null);
    }

    @Test
    void aSentBatchEndsOnlyWhenTheDaEndsIt() {
        List<DaSyncQueueItem> batch = List.of(item("a", QueueItemState.EXPORT_SENT, "e1"));
        when(daService.getDueBatch(QueueItemState.EXPORT_SENT)).thenReturn(batch);
        when(daService.ingestStatusFinished(repository, "e1")).thenThrow((RuntimeException) daFailure(500));

        assertFalse(exportProcessor.checkSentBatch());
        verify(daService).scheduleRetry(eq(batch), anyString());
        verify(daService, never()).changeQueueItemsState(any(), any(), any());

        doThrow((RuntimeException) daFailure(404)).when(daService).ingestStatusFinished(repository, "e1");
        assertFalse(exportProcessor.checkSentBatch());
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(daService).changeQueueItemsState(eq(batch), eq(QueueItemState.EXPORT_ERROR), message.capture());
        assertTrue(message.getValue().startsWith("Digitální archiv zpracování balíčku ukončil:"), message.getValue());
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
