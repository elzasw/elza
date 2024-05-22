package cz.tacr.elza.service;

import cz.tacr.elza.domain.DaSyncQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DaExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaExtSyncsProcessor.class);

    @Autowired
    private DaService daService;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static final int QUEUE_CHECK_TIME_INTERVAL = 10000;

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
                this.asyncThread = new Thread(this,"DaExtSyncsProcessor");
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
                    try {
                        Iterable<DaSyncQueueItem> syncQueueItemList = daService.getNextItems(importListSize, DaSyncQueueItem.QueueItemState.UPDATE, DaSyncQueueItem.QueueItemState.IMPORT_NEW);
                        if (syncQueueItemList.iterator().hasNext()) {
//                            itemProcessor.process(); //todo fantis stažení
                            // pokud je vše v pořádku - maximální velikost dávky pro čtení
                            importListSize = DEFAULT_IMPORT_LIST_SIZE;
                            // pauza po ukončení práce procesoru není potřeba
                            wait = false;
                        }
                    } catch (Exception ex) {
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
                logger.error("DaExtSyncsProcessor - processor thread error", e);
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
            logger.error("DaExtSyncsProcessor - thread finished");
        }
    }

}
