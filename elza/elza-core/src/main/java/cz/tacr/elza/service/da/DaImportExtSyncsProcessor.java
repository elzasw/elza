package cz.tacr.elza.service.da;

import cz.tacr.da.ApiException;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaDownloadMethod;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.service.ExternalSystemService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cz.tacr.elza.connector.DaConnector.FILE_TRANSFER_ERROR_CODE;
import cz.tacr.elza.api.DaOnReceivedAction;
import java.util.Map;
import java.util.Set;

@Component
public class DaImportExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaImportExtSyncsProcessor.class);

    @Autowired
    private DaService daService;
    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private DaAipAutoLinkService aipAutoLinkService;

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
    private Path downloadBatch(ArrDigitalRepository digitalRepository, String batchId) throws ApiException, IOException {
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
                            String batchId = daService.downloadAips(digitalRepository, syncQueueItemList, aipType);

                            while (!daService.downloadStatusFinished(digitalRepository, batchId)) {
                                try {
                                    // wake up every minute to retry
                                    lock.wait(DOWNLOAD_CHECK_TIME_INTERVAL);
                                } catch (InterruptedException e) {
                                    logger.error(e.getMessage(), e);
                                    break;
                                }
                            }

                            Path zipFile = downloadBatch(digitalRepository, batchId);

                            try (InputStream inputStream  = Files.newInputStream(zipFile)) {
                                daService.processPackageInfo(digitalRepository, inputStream, aipType, syncQueueItemList);
                            }
                            Files.delete(zipFile);

                            daService.updateAipToQueueItems(syncQueueItemList);

                            boolean autoProcess = digitalRepository.getOnReceived() == DaOnReceivedAction.DOWNLOAD_METADATA;
                            List<Integer> receivedAipIds = autoProcess && aipType == AipType.PACKAGE_INFO
                                    ? receivedAipIds(syncQueueItemList) : List.of();
                            if (aipType == AipType.METADATA_BASE || aipType == AipType.AIP_BASE) {
                                List<Integer> aipids = syncQueueItemList.stream().map(q -> q.getAip().getAipId()).toList();
                                Map<Integer, Set<String>> identifiersByAip = daService.doCreateDaoStructure(aipids, false);
                                if (autoProcess) {
                                    aipAutoLinkService.linkReceivedAips(identifiersByAip);
                                }
                            }

                            daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.IMPORT_OK);

                            // Enqueued only after the batch is saved as IMPORT_OK: the new queue
                            // item deactivates the AIP's previous items and a later save of the
                            // batch entities must not restore their active flag.
                            requestMetadataOfReceivedAips(receivedAipIds);

                            // pokud je vše v pořádku - maximální velikost dávky pro čtení
                            importListSize = DEFAULT_IMPORT_LIST_SIZE;
                            // pauza po ukončení práce procesoru není potřeba
                            wait = false;
                        }
                    } catch (Exception ex) {
                        daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.IMPORT_ERROR);

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
