package cz.tacr.elza.service.da;

import com.lightcomp.ft.client.Transfer;
import com.lightcomp.ft.client.TransferState;
import cz.tacr.da.controller.vo.IngestIngestResult;
import cz.tacr.da.controller.vo.IngestPackageIngestError;
import cz.tacr.da.controller.vo.IngestPackageIngestSuccess;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.da.vo.DaUploadRequestImpl;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sends change packages to the DA.
 *
 * An export is two kinds of exchange with the DA, each a short step under
 * {@link DaCommunicationLock}: sending a batch, after which its items wait as
 * {@link DaSyncQueueItem.QueueItemState#EXPORT_SENT}, and asking whether the DA has ingested it -
 * once per status interval - until its result is read. While the DA ingests, the lock is free for
 * the synchronization and the downloads. One batch per repository is in flight at a time.
 *
 * A sent batch is never given up by ELZA: only the DA ends it, with a result or with a permanent
 * failure (403/404). A failure to reach the DA is retried with a growing delay.
 */
@Component
public class DaExportExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaExportExtSyncsProcessor.class);

    @Autowired
    private DaService daService;
    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private DaAipActionService actionService;
    @Autowired
    private DaCommunicationLock communicationLock;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static final long QUEUE_CHECK_TIME_INTERVAL = 10000;

    private static final int TRANSFER_CHECK_TIME_INTERVAL = 100;

    private static final int DEFAULT_EXPORT_LIST_SIZE = 100;

    private static final List<DaSyncQueueItem.QueueItemState> QUEUE_STATES = List.of(
            DaSyncQueueItem.QueueItemState.EXPORT_NEW,
            DaSyncQueueItem.QueueItemState.EXPORT_SENT);

    static final String NOT_CONFIRMED = "Digitální archiv nepotvrdil přijetí balíčku.";

    private int exportListSize = DEFAULT_EXPORT_LIST_SIZE;

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private ThreadStatus status;

    public void startExtSyncs() {
        synchronized (lock) {
            status = ThreadStatus.RUNNING;
            if (this.asyncThread == null) {
                this.asyncThread = new Thread(this,"DaExportExtSyncsProcessor");
                this.asyncThread.start();
            }
        }
    }

    /**
     * Sends the waiting change packages to the DA as one batch. A package ELZA cannot read is
     * an error of the export; a failed upload is retried after a growing delay.
     *
     * @return true when a batch was sent
     */
    boolean sendNextBatch() {
        List<DaSyncQueueItem> items = daService.getNextItems(exportListSize,
                DaSyncQueueItem.QueueItemState.EXPORT_SENT, DaSyncQueueItem.QueueItemState.EXPORT_NEW);
        if (items.isEmpty()) {
            return false;
        }
        ArrDigitalRepository digitalRepository = repositoryOf(items);
        Path exportDir;
        try {
            exportDir = daService.createOutputDir(items);
        } catch (Exception ex) {
            String failure = AipProblem.of(ex).description();
            daService.changeQueueItemsState(items, DaSyncQueueItem.QueueItemState.EXPORT_ERROR, failure);
            actionService.completeFromQueue(items, DaAipActionItemState.ERROR, failure);
            logger.error("Failed to prepare the export batch of {} item(s)", items.size(), ex);
            exportListSize = 1;
            return false;
        }
        try {
            String batchId = upload(digitalRepository, exportDir);
            daService.markBatch(items, DaSyncQueueItem.QueueItemState.EXPORT_SENT, batchId, digitalRepository);
            logger.debug("Sent export batch {} of {} item(s)", batchId, items.size());
            exportListSize = DEFAULT_EXPORT_LIST_SIZE;
            return true;
        } catch (Exception ex) {
            logger.error("Failed to send the export batch of {} item(s), it will be sent again.", items.size(), ex);
            daService.scheduleRetry(items, "Změnový balíček se nepodařilo odeslat do digitálního archivu: "
                    + DaRequestFailure.describe(ex) + ". Odeslání bude opakováno.");
            exportListSize = 1;
            return false;
        } finally {
            try {
                PathUtils.deleteDirectory(exportDir);
            } catch (IOException e) {
                logger.warn("Failed to delete the export directory {}: {}", exportDir, e.getMessage());
            }
        }
    }

    /**
     * Uploads the batch over File Transfer. The upload runs in this process and is bounded by the
     * size of the change packages, not by the DA.
     *
     * @return the id of the batch the DA received
     */
    private String upload(ArrDigitalRepository digitalRepository, Path exportDir) throws InterruptedException {
        DaUploadRequestImpl daUploadRequest = daService.createDaUploadRequest(exportDir);
        Transfer transfer = daService.ingestFileTransfer(digitalRepository, daUploadRequest);
        while (transfer.getStatus().getState() != TransferState.FINISHED
                && transfer.getStatus().getState() != TransferState.FAILED
                && transfer.getStatus().getState() != TransferState.CANCELED) {
            Thread.sleep(TRANSFER_CHECK_TIME_INTERVAL);
        }
        if (transfer.getStatus().getState() != TransferState.FINISHED || daUploadRequest.getResponse() == null) {
            throw new IllegalStateException("Přenos souborů neproběhl");
        }
        return daUploadRequest.getResponse().getId();
    }

    /**
     * Asks the DA once about the sent batch whose question is due, and records its result when
     * the DA has ingested it.
     *
     * @return true when a result was recorded
     */
    boolean checkSentBatch() {
        List<DaSyncQueueItem> batch = daService.getDueBatch(DaSyncQueueItem.QueueItemState.EXPORT_SENT);
        if (batch.isEmpty()) {
            return false;
        }
        ArrDigitalRepository digitalRepository = repositoryOf(batch);
        String batchId = batch.get(0).getBatchId();
        IngestIngestResult result;
        try {
            if (!daService.ingestStatusFinished(digitalRepository, batchId)) {
                daService.scheduleNextCheck(batch, digitalRepository);
                return false;
            }
            result = daService.ingestResult(digitalRepository, batchId);
        } catch (Exception ex) {
            String description = DaRequestFailure.describe(ex);
            if (DaRequestFailure.isPermanent(ex)) {
                String failure = "Digitální archiv zpracování balíčku ukončil: " + description;
                logger.warn("The DA gave up export batch {}: {}", batchId, description);
                daService.changeQueueItemsState(batch, DaSyncQueueItem.QueueItemState.EXPORT_ERROR, failure);
                actionService.completeFromQueue(batch, DaAipActionItemState.ERROR, failure);
            } else {
                logger.error("Failed to ask about export batch {}, it will be asked again.", batchId, ex);
                daService.scheduleRetry(batch, "Stav odeslaného balíčku se nepodařilo zjistit: " + description
                        + ". Dotaz bude opakován.");
            }
            return false;
        }
        recordResult(batch, result);
        return true;
    }

    /**
     * Each package of the batch ends by what the DA said about it: accepted, refused with its
     * reason, or - when the DA did not mention it - not confirmed.
     */
    private void recordResult(List<DaSyncQueueItem> batch, IngestIngestResult result) {
        Map<String, String> accepted = new HashMap<>();
        Map<String, String> refused = new HashMap<>();
        if (result != null) {
            for (IngestPackageIngestSuccess success : result.getAccepted()) {
                accepted.put(success.getSipId(), success.getAipVersion());
                accepted.putIfAbsent(success.getAipId(), success.getAipVersion());
            }
            for (IngestPackageIngestError error : result.getPackageIngestError()) {
                refused.put(error.getSipId(), error.getErrorInfo() == null ? null
                        : DaRequestFailure.errorInfo(error.getErrorInfo().getErrorTitle(),
                                                     error.getErrorInfo().getErrorDetail(),
                                                     error.getErrorInfo().getErrorCode()));
            }
        }
        for (DaSyncQueueItem item : batch) {
            String code = item.getCode();
            if (accepted.containsKey(code)) {
                String version = accepted.get(code);
                daService.changeQueueItemsState(List.of(item), DaSyncQueueItem.QueueItemState.EXPORT_OK,
                        version == null ? null : "Přijato jako verze " + version + ".");
                actionService.completeFromQueue(List.of(item), DaAipActionItemState.FINISHED, null);
            } else {
                String failure = refused.containsKey(code)
                        ? "Digitální archiv balíček odmítl: " + (refused.get(code) == null ? "bez udání důvodu" : refused.get(code))
                        : NOT_CONFIRMED;
                daService.changeQueueItemsState(List.of(item), DaSyncQueueItem.QueueItemState.EXPORT_ERROR, failure);
                actionService.completeFromQueue(List.of(item), DaAipActionItemState.ERROR, failure);
            }
        }
    }

    private ArrDigitalRepository repositoryOf(List<DaSyncQueueItem> items) {
        return externalSystemService.getDigitalRepository(items.get(0).getDigitalRepository().getExternalSystemId());
    }

    /** One exchange with the DA: from reading the queue to saving what it brought. */
    private boolean exchange(Supplier<Boolean> step) {
        communicationLock.lock();
        try {
            return step.get();
        } catch (Exception e) {
            logger.error("DaExportExtSyncsProcessor - step failed", e);
            return false;
        } finally {
            communicationLock.unlock();
        }
    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
                while (status == ThreadStatus.RUNNING) {
                    boolean recorded = exchange(this::checkSentBatch);
                    boolean sent = exchange(this::sendNextBatch);
                    if (!recorded && !sent) {
                        try {
                            lock.wait(DaService.idleMillis(daService.getEarliestAttempt(QUEUE_STATES), QUEUE_CHECK_TIME_INTERVAL));
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("DaExportExtSyncsProcessor - processor thread error", e);
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
            logger.error("DaExportExtSyncsProcessor - thread finished");
        }
    }

}
