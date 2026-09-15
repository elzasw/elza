package cz.tacr.elza.service.da;

import cz.tacr.da.ApiException;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaDownloadMethod;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.service.ExternalSystemService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.tacr.elza.connector.DaConnector.FILE_TRANSFER_ERROR_CODE;
import cz.tacr.elza.api.DaOnReceivedAction;
import java.util.Map;
import java.util.Set;
import cz.tacr.elza.common.io.SpooledContent;

@Component
public class DaImportExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaImportExtSyncsProcessor.class);

    @Autowired
    private DaService daService;
    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private DaAipAutoLinkService aipAutoLinkService;
    @Autowired
    private DaAipActionService actionService;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static final int QUEUE_CHECK_TIME_INTERVAL = 10000;

    private static final int DOWNLOAD_CHECK_TIME_INTERVAL = 100;

    private static final int DEFAULT_IMPORT_LIST_SIZE = 100;

    private int importListSize = DEFAULT_IMPORT_LIST_SIZE;

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private ThreadStatus status;

    public void startExtSyncs() {
        synchronized (lock) {
            status = ThreadStatus.RUNNING;
            if (this.asyncThread == null) {
                this.asyncThread = new Thread(this,"DaImportExtSyncsProcessor");
                this.asyncThread.start();
            }
        }
    }

    /**
     * Downloads the prepared batch using the method configured on the repository.
     * The standard HTTP download is refused by the DA with 413 when the batch is too
     * large; in that case the administrator has to switch the repository to File Transfer.
     */
    private SpooledContent downloadBatch(ArrDigitalRepository digitalRepository, String batchId) throws ApiException, IOException {
        DaDownloadMethod method = digitalRepository.getDownloadMethod() == null
                ? DaDownloadMethod.STANDARD : digitalRepository.getDownloadMethod();
        if (method == DaDownloadMethod.FILE_TRANSFER) {
            return daService.downloadFileTransfer(digitalRepository, batchId);
        }
        try {
            return daService.downloadDownload(digitalRepository, batchId);
        } catch (ApiException e) {
            if (e.getCode() == FILE_TRANSFER_ERROR_CODE) {
                throw new IllegalStateException("Repository " + digitalRepository.getCode()
                        + " refused the standard download of batch " + batchId
                        + " (HTTP 413); switch the repository download method to File Transfer.", e);
            }
            throw e;
        }
    }

    /**
     * Obtains the package batch of the given queue items from the DA: requests its preparation,
     * waits for it and downloads it. A failure anywhere here means nothing of the package
     * arrived, so it closes no item: the items stay pending to be retried on a later cycle,
     * the failure is recorded on them and on their AIPs, and null is returned.
     *
     * Must be called while holding {@link #lock}, which paces the polling for the batch.
     */
    private SpooledContent obtainBatch(ArrDigitalRepository digitalRepository,
                                       List<DaSyncQueueItem> syncQueueItemList, AipType aipType) {
        try {
            String batchId = daService.downloadAips(digitalRepository, syncQueueItemList, aipType);

            while (!daService.downloadStatusFinished(digitalRepository, batchId)) {
                try {
                    lock.wait(DOWNLOAD_CHECK_TIME_INTERVAL);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    break;
                }
            }

            return downloadBatch(digitalRepository, batchId);
        } catch (Exception ex) {
            logger.error("Failed to download the batch of {} queue item(s), the items will be retried.",
                         syncQueueItemList.size(), ex);
            daService.recordDownloadFailure(syncQueueItemList, ex);
            return null;
        }
    }

    /**
     * Requests the metadata package of the AIPs that have just been received (queue items in
     * state IMPORT_NEW) - the repository is configured to download metadata automatically. The
     * metadata import later attaches the AIP to its node.
     */
    private void requestMetadataOfReceivedAips(List<Integer> receivedAipIds) {
        if (!receivedAipIds.isEmpty()) {
            logger.info("Requesting metadata of {} received AIP(s): {}", receivedAipIds.size(), receivedAipIds);
            daService.createDaoStructure(receivedAipIds);
        }
    }

    /**
     * The one problem of the whole batch, described on each of its items - the batch failed as
     * a whole and nothing tells its items apart.
     */
    private static Map<DaSyncQueueItem, AipProblem> problemPerItem(@Nullable List<DaSyncQueueItem> syncQueueItemList,
                                                                   AipProblem problem) {
        return CollectionUtils.isEmpty(syncQueueItemList)
                ? Map.of()
                : syncQueueItemList.stream().collect(Collectors.toMap(Function.identity(), item -> problem));
    }

    /**
     * @return ids of the AIPs the batch has just received (queue items in state IMPORT_NEW
     *         whose PACKAGE-INFO created the AIP)
     */
    private static List<Integer> receivedAipIds(List<DaSyncQueueItem> syncQueueItemList) {
        return syncQueueItemList.stream()
                .filter(q -> q.getState() == DaSyncQueueItem.QueueItemState.IMPORT_NEW && q.getAip() != null)
                .map(q -> q.getAip().getAipId())
                .toList();
    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
                while (status == ThreadStatus.RUNNING) {
                    // pokud true - pauza po ukončení práce procesoru
                    boolean wait = true;
                    List<DaSyncQueueItem> syncQueueItemList = null;
                    try {
                        syncQueueItemList = daService.getNextItems(importListSize, DaSyncQueueItem.QueueItemState.UPDATE, DaSyncQueueItem.QueueItemState.IMPORT_NEW);
                        if (CollectionUtils.isNotEmpty(syncQueueItemList)) {
                            DaSyncQueueItem firstQueueItem = syncQueueItemList.get(0);
                            Integer digitalRepositoryId = firstQueueItem.getDigitalRepository().getExternalSystemId();
                            AipType aipType = firstQueueItem.getAipType();
                            ArrDigitalRepository digitalRepository = externalSystemService.getDigitalRepository(digitalRepositoryId);
                            SpooledContent zip = obtainBatch(digitalRepository, syncQueueItemList, aipType);
                            if (zip == null) {
                                // download failed, the items stay pending for the next cycle;
                                // v případě chyby číst po 1 záznamu
                                importListSize = 1;
                            } else {
                                try (zip; InputStream inputStream = zip.openStream()) {
                                    daService.processPackageInfo(digitalRepository, inputStream, aipType, syncQueueItemList);
                                }

                                daService.updateAipToQueueItems(syncQueueItemList);

                                boolean autoProcess = digitalRepository.getOnReceived() == DaOnReceivedAction.DOWNLOAD_METADATA;
                                List<Integer> receivedAipIds = autoProcess && aipType == AipType.PACKAGE_INFO
                                        ? receivedAipIds(syncQueueItemList) : List.of();
                                if (aipType == AipType.METADATA_BASE || aipType == AipType.AIP_BASE) {
                                    List<Integer> aipids = syncQueueItemList.stream().map(q -> q.getAip().getAipId()).toList();
                                    Map<Integer, List<String>> uuidsByAip = daService.doCreateDaoStructure(aipids, false,
                                            actionService.sinkForQueueItems(syncQueueItemList));
                                    if (autoProcess) {
                                        aipAutoLinkService.linkReceivedAips(uuidsByAip);
                                    }
                                }

                                daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.IMPORT_OK);
                                actionService.completeFromQueue(syncQueueItemList, DaAipActionItemState.FINISHED, null);

                                // Enqueued only after the batch is saved as IMPORT_OK: the new queue
                                // item deactivates the AIP's previous items and a later save of the
                                // batch entities must not restore their active flag.
                                requestMetadataOfReceivedAips(receivedAipIds);

                                // pokud je vše v pořádku - maximální velikost dávky pro čtení
                                importListSize = DEFAULT_IMPORT_LIST_SIZE;
                                // pauza po ukončení práce procesoru není potřeba
                                wait = false;
                            }
                        }
                    } catch (Exception ex) {
                        // The package was in hand and its processing failed, so - unlike a failed
                        // download - the items are closed; the problem is written on their AIPs
                        // and their action items as well, because a terminal failure the user can
                        // only find in the queue is a failure they do not find.
                        AipProblem problem = AipProblem.of(ex);
                        daService.failQueueItems(problemPerItem(syncQueueItemList, problem),
                                                 DaSyncQueueItem.QueueItemState.IMPORT_ERROR);

                        logger.error("Failed to process item. ", ex);
                        // v případě chyby číst po 1 záznamu
                        importListSize = 1;
                    }
                    if (wait) {
                        try {
                            // wake up every minute to retry
                            lock.wait(QUEUE_CHECK_TIME_INTERVAL);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("DaImportExtSyncsProcessor - processor thread error", e);
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
            logger.error("DaImportExtSyncsProcessor - thread finished");
        }
    }

}
