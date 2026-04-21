package cz.tacr.elza.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.cam.ItemSyncProcessor;

@Component
public class ExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ExtSyncsProcessor.class);

    @Autowired
    private AccessPointConnectorService apConnectorService;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private final static int QUEUE_CHECK_TIME_INTERVAL = 10000;

    private final static int DEFAULT_IMPORT_LIST_SIZE = 100;

    private int importListSize = DEFAULT_IMPORT_LIST_SIZE;

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private ThreadStatus status;

    public void startExtSyncs() {
        synchronized (lock) {
            status = ThreadStatus.RUNNING;
            if (this.asyncThread == null) {
                this.asyncThread = new Thread(this,"ExtSyncsProcessor");
                this.asyncThread.start();
            }
        }
    }

    /**
     * Signals the worker thread to stop and waits for it to finish.
     * Intended for tests that drive {@link AccessPointConnectorService#nextItemSyncProcessor}
     * synchronously and must not race with the background processor over the same queue item.
     * No-op when the thread has not been started.
     */
    public void stopExtSyncs() {
        Thread threadToJoin;
        synchronized (lock) {
            if (asyncThread == null) {
                return;
            }
            threadToJoin = asyncThread;
            status = ThreadStatus.STOP_REQUEST;
            lock.notifyAll();
        }
        try {
            threadToJoin.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (lock) {
            asyncThread = null;
        }
    }

    @Override
    public void run() {
    	logger.info("ExtSyncsProcessor - thread started.");
        synchronized (lock) {
            try {
                while (status == ThreadStatus.RUNNING) {
                	logger.trace("ExtSyncsProcessor - activating.");
                    // pokud true - pauza po ukončení práce procesoru
                    boolean wait = true;
                    try {
                        ItemSyncProcessor itemProcessor = apConnectorService.nextItemSyncProcessor(importListSize);
                        if (itemProcessor != null) {
                        	logger.trace("ExtSyncsProcessor - processing item: {}.", itemProcessor.toString());
                            if(itemProcessor.process()) {
                                // pokud je vše v pořádku - maximální velikost dávky pro čtení
                                importListSize = DEFAULT_IMPORT_LIST_SIZE;
                                // pauza po ukončení práce procesoru není potřeba
                                wait = false;	                            
                            }
                        } else {
                        	logger.trace("ExtSyncsProcessor - no item to process.");
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to process item. ", ex);
                        // v případě chyby číst po 1 záznamu
                        importListSize = 1;
                    }
                    if (wait) {
                        try {
                        	logger.trace("ExtSyncsProcessor - waiting.");
                            // wake up every minute to retry
                            lock.wait(QUEUE_CHECK_TIME_INTERVAL);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("ExtSyncsProcessor - processor thread error " + e.toString());
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();            
        }
        logger.info("ExtSyncsProcessor - thread finished.");
    }

}
