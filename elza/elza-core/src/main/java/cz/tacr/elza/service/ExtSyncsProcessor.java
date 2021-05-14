package cz.tacr.elza.service;

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

import java.util.List;

@Component
public class ExtSyncsProcessor implements Runnable {

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private CamService camService;

    private static final Logger logger = LoggerFactory.getLogger(ExtSyncsProcessor.class);

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static int QUEUE_CHECK_TIME_INTERVAL = 10000;

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
        Page<ExtSyncsQueueItem> newToElza = extSyncsQueueItemRepository.findByState(ExtSyncsQueueItem.ExtAsyncQueueState.IMPORT_NEW, pageable);
        if (!newToElza.isEmpty()) {
            List<ExtSyncsQueueItem> items = newToElza.getContent();
            for (ExtSyncsQueueItem item : items) {
                if (!camService.synchronizeIntItem(item)) {
                    return false;
                }
            }
        }

        // add new items from ELZA
        Page<ExtSyncsQueueItem> newFromElza = extSyncsQueueItemRepository.findByState(ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_NEW, pageable);
        if (newFromElza.isEmpty()) {
            return false;
        }
        List<ExtSyncsQueueItem> items = newFromElza.getContent();
        for (ExtSyncsQueueItem item : items) {
            if (!camService.synchronizeExtItem(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
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
