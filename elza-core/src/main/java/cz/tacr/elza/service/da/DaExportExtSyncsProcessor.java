package cz.tacr.elza.service.da;

import com.lightcomp.ft.client.Transfer;
import com.lightcomp.ft.client.TransferState;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.service.ExternalSystemService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DaExportExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaExportExtSyncsProcessor.class);

    @Autowired
    private DaService daService;
    @Autowired
    private ExternalSystemService externalSystemService;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static final int QUEUE_CHECK_TIME_INTERVAL = 10000;

    private static final int DOWNLOAD_CHECK_TIME_INTERVAL = 100;

    private static final int DEFAULT_EXPORT_LIST_SIZE = 100;

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

    @Override
    public void run() {
        synchronized (lock) {
            try {
                while (status == ThreadStatus.RUNNING) {
                    // pokud true - pauza po ukončení práce procesoru
                    boolean wait = true;
                    List<DaSyncQueueItem> syncQueueItemList = null;
                    try {
                        syncQueueItemList = daService.getNextItems(exportListSize, DaSyncQueueItem.QueueItemState.EXPORT_NEW);
                        if (CollectionUtils.isNotEmpty(syncQueueItemList)) {
                            Integer digitalRepositoryId = syncQueueItemList.get(0).getDigitalRepository().getExternalSystemId();
                            ArrDigitalRepository digitalRepository = externalSystemService.getDigitalRepository(digitalRepositoryId);
                            Path exportDir = daService.createOutputDir(syncQueueItemList);
                            Transfer transfer = daService.ingestFileTransfer(digitalRepository, exportDir);

                            while (transfer.getStatus().getState() != TransferState.FINISHED
                                    && transfer.getStatus().getState() != TransferState.FAILED
                                    && transfer.getStatus().getState() != TransferState.CANCELED) {
                                try {
                                    // wake up every minute to retry
                                    lock.wait(DOWNLOAD_CHECK_TIME_INTERVAL);
                                } catch (InterruptedException e) {
                                    logger.error(e.getMessage(), e);
                                    break;
                                }

                            }

                            if ((transfer.getStatus().getState() == TransferState.FAILED || transfer.getStatus().getState() == TransferState.CANCELED)) {
                                throw new IllegalStateException("Přenos souborů neproběhl");
                            }

                            String batchId = transfer.getTransferId();

                            while (!daService.ingestStatusFinished(digitalRepository, batchId)) {
                                try {
                                    // wake up every minute to retry
                                    lock.wait(DOWNLOAD_CHECK_TIME_INTERVAL);
                                } catch (InterruptedException e) {
                                    logger.error(e.getMessage(), e);
                                    break;
                                }
                            }

                            List<String> successfullAipIds = daService.ingestResult(digitalRepository, batchId);

                            // Odstranit dočasné soubory a adresáře
                            try (Stream<Path> str = Files.walk(exportDir)) {
                                str.map(Path::toFile).forEach(File::delete);
                            }

                            List<DaSyncQueueItem> successfull = syncQueueItemList.stream()
                                    .filter(q -> successfullAipIds.contains(q.getCode()))
                                    .collect(Collectors.toList());
                            Collection<DaSyncQueueItem> error = CollectionUtils.subtract(syncQueueItemList, successfull);

                            daService.changeQueueItemsState(successfull, DaSyncQueueItem.QueueItemState.EXPORT_OK);
                            daService.changeQueueItemsState(error, DaSyncQueueItem.QueueItemState.EXPORT_ERROR);

                            // pokud je vše v pořádku - maximální velikost dávky pro čtení
                            exportListSize = DEFAULT_EXPORT_LIST_SIZE;
                            // pauza po ukončení práce procesoru není potřeba
                            wait = false;
                        }
                    } catch (Exception ex) {
                        daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.EXPORT_ERROR);

                        logger.error("Failed to process item. ", ex);
                        // v případě chyby číst po 1 záznamu
                        exportListSize = 1;
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
                logger.error("DaExportExtSyncsProcessor - processor thread error", e);
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
            logger.error("DaExportExtSyncsProcessor - thread finished");
        }
    }

}
