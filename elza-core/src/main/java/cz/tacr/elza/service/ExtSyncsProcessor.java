package cz.tacr.elza.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.service.cam.CamService;
import cz.tacr.elza.service.cam.ItemSyncProcessor;

@Component
public class ExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ExtSyncsProcessor.class);

    @Autowired
    private CamService camService;

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

//    private boolean processItem() {
//        Pageable pageable = PageRequest.of(0, 1);
//        // sync updated items from ExtSystem
//        Page<ExtSyncsQueueItem> updPage = extSyncsQueueItemRepository.findByState(ExtAsyncQueueState.UPDATE, pageable);
//        if (!updPage.isEmpty()) {
//            List<ExtSyncsQueueItem> items = updPage.getContent();
//            for (ExtSyncsQueueItem item : items) {
////                if (!camService.synchronizeIntItem(item)) {
////                    return false;
////                }
//            }
//            return true;
//        }
//
//        // add new item to Elza
//        Pageable pageImport = PageRequest.of(0, importListSize);
//        Page<ExtSyncsQueueItem> newToElza = extSyncsQueueItemRepository.findByState(ExtAsyncQueueState.IMPORT_NEW, pageImport);
//        if (!newToElza.isEmpty()) {
//            List<ExtSyncsQueueItem> items = newToElza.getContent();
////            try {
////                camService.importNew(items);
////                // návrat standardní dávky po úspěšném zpracování
////                importListSize = DEFAULT_IMPORT_LIST_SIZE;
////            } catch (ApiException e) {
////                // if ApiException -> it means we connected server and it is logical failure 
////                logger.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
////                // pokud došlo k chybě při čtení 1 záznam najednou
////                if (items.size() == 1) {
////                    // check if item not found
////                    if (e.getCode() == 404) {
////                    	camService.setQueueItemStateTA(items,
////                    			ExtAsyncQueueState.ERROR,
////                    			OffsetDateTime.now(),
////                    			ExceptionUtils.getApiExceptionInfo(e));
////                    	return true;
////                    }
////                    // we can retry later
////                	camService.setQueueItemStateTA(items,
////                			null,
////                			OffsetDateTime.now(),
////                			ExceptionUtils.getApiExceptionInfo(e));
////                	return false;
////                } else {
////                    // zmenšení velikosti dávky
////                    importListSize = 1;
////                    return true;
////                }                
////            } catch (Exception e) {
////                // handling other errors -> if it is one record - write the error
////                logger.error("Failed to synchronize item(s), list size: {}", items.size(), e);
////                // pokud došlo k chybě při čtení 1 záznam najednou
////                if (items.size() == 1) {
////                	// TODO: rework this as not permanent error but try same operation later
////                	// In general this should not happend - some kind of inconsistency?
////                    camService.setQueueItemStateTA(items,
////                                                   ExtAsyncQueueState.ERROR, 
////                                                   OffsetDateTime.now(),
////                                                   e.getMessage());
////                    return true;
////                } else {
////                    // zmenšení velikosti dávky
////                    importListSize = 1;                	
////                }
////                return true;
////            }
////            return true;
//        }
//
//        // add new items from ELZA
//        List<ExtAsyncQueueState> exportNewOrStart = Arrays.asList(ExtAsyncQueueState.EXPORT_NEW, ExtAsyncQueueState.EXPORT_START);
//        Page<ExtSyncsQueueItem> newFromElza = extSyncsQueueItemRepository.findByStates(exportNewOrStart, pageable);
//        if (newFromElza.isEmpty()) {
//            return false;
//        }
//        List<ExtSyncsQueueItem> items = newFromElza.getContent();
//        for (ExtSyncsQueueItem item : items) {
////            if (!upload(item)) {
////                return false;
////            }
//        }
//        return true;
//    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
                while (status == ThreadStatus.RUNNING) {
                    // pokud true - pauza po ukončení práce procesoru
                    boolean wait = true;
                    try {
                        ItemSyncProcessor itemProcessor = camService.nextItemSyncProcessor(importListSize);
                        if (itemProcessor != null) {
                            itemProcessor.process();
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
            logger.error("ExtSyncsProcessor - thread finished");
        }
    }

}
