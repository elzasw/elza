package cz.tacr.elza.service.da;

import cz.tacr.da.ApiException;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cz.tacr.elza.connector.DaConnector.FILE_TRANSFER_ERROR_CODE;

@Component
public class DaImportExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaImportExtSyncsProcessor.class);

    @Autowired
    private DaService daService;

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
                            ArrDigitalRepository digitalRepository = syncQueueItemList.get(0).getDigitalRepository();
                            AipType aipType = AipType.METADATA_BASE;
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

                            Path zipFile;
                            try {
                                zipFile = daService.downloadDownload(digitalRepository, batchId);
                            } catch (ApiException e) {
                                if (e.getCode() == FILE_TRANSFER_ERROR_CODE) {
                                    zipFile = daService.downloadFileTransfer(digitalRepository, batchId);
                                } else {
                                    throw e;
                                }
                            }

                            try (InputStream inputStream  = Files.newInputStream(zipFile)) {
                                daService.processPackageInfo(digitalRepository, inputStream, aipType);
                            }
                            Files.delete(zipFile);

                            daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.IMPORT_OK);

                            // pokud je vše v pořádku - maximální velikost dávky pro čtení
                            importListSize = DEFAULT_IMPORT_LIST_SIZE;
                            // pauza po ukončení práce procesoru není potřeba
                            wait = false;
                        }
                    } catch (Exception ex) {
                        daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.ERROR);

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
