package cz.tacr.elza.service;

import cz.tacr.cam.client.ApiException;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.cam.CamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ExtSyncsProcessor implements Runnable {

    @Autowired
    private CamService camService;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    private static final Logger logger = LoggerFactory.getLogger(ExtSyncsProcessor.class);

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static int QUEUE_CHECK_TIME_INTERVAL = 10000;

    private static int DEFAULT_IMPORT_LIST_SIZE = 100;

    private int importListSize;

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

    private boolean processItem() {
        Pageable pageable = PageRequest.of(0, 1);
        // sync updated items from ExtSystem
        Page<ExtSyncsQueueItem> updPage = extSyncsQueueItemRepository.findByState(ExtSyncsQueueItem.ExtAsyncQueueState.UPDATE, pageable);
        if (!updPage.isEmpty()) {
            List<ExtSyncsQueueItem> items = updPage.getContent();
            for (ExtSyncsQueueItem item : items) {
                if (!camService.synchronizeIntItem(item)) {
                    return false;
                }
            }
            return true;
        }

        // add new item to Elza
        Pageable pageImport = PageRequest.of(0, importListSize);
        Page<ExtSyncsQueueItem> newToElza = extSyncsQueueItemRepository.findByState(ExtSyncsQueueItem.ExtAsyncQueueState.IMPORT_NEW, pageImport);
        if (!newToElza.isEmpty()) {
            List<ExtSyncsQueueItem> items = newToElza.getContent();
            try {
                camService.importNew(items);
            } catch (ApiException e) {
                // if ApiException -> it means we connected server and it is logical failure 
                logger.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
                new TransactionTemplate(transactionManager).execute(status -> {
                    camService.setQueueItemState(items,
                                                 null, // state se nemění
                                                 OffsetDateTime.now(),
                                                 e.getMessage());
                    return true;
                });
                return false;
            } catch (Exception e) {
                // handling other errors -> if it is one record - write the error
                logger.error("Failed to synchronize item(s), list size: {}", items.size(), e);
                // zmenšení velikosti dávky
                importListSize = 1;
                // pokud došlo k chybě při čtení 1 záznam najednou
                if (items.size() == 1) {
                    new TransactionTemplate(transactionManager).execute(status -> {
                        camService.setQueueItemState(items,
                                                     ExtSyncsQueueItem.ExtAsyncQueueState.ERROR, 
                                                     OffsetDateTime.now(),
                                                     e.getMessage());
                        return true;
                    });
                    // chybný záznam je označen, návrat standardní dávky
                    importListSize = DEFAULT_IMPORT_LIST_SIZE;
                    return true;
                }
                return true;
            }
            return true;
        }

        // add new items from ELZA
        Page<ExtSyncsQueueItem> newFromElza = extSyncsQueueItemRepository.findByState(ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_NEW, pageable);
        if (newFromElza.isEmpty()) {
            return false;
        }
        List<ExtSyncsQueueItem> items = newFromElza.getContent();
        for (ExtSyncsQueueItem item : items) {
            if (!camService.exportNew(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
                importListSize = DEFAULT_IMPORT_LIST_SIZE;
                while (status == ThreadStatus.RUNNING) {
                    boolean wait = true;
                    try {
                        if (processItem()) {
                            wait = false;
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to process item. ", ex);
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
