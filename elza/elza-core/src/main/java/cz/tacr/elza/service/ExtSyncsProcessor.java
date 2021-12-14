package cz.tacr.elza.service;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.ErrorMessageXml;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.cam.CamService;
import cz.tacr.elza.service.cam.UploadWorker;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.util.Collections;
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
                // návrat standardní dávky po úspěšném zpracování
                importListSize = DEFAULT_IMPORT_LIST_SIZE;
            } catch (ApiException e) {
                // if ApiException -> it means we connected server and it is logical failure 
                logger.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
                // pokud došlo k chybě při čtení 1 záznam najednou
                if (items.size() == 1) {
                    // check if item not found
                    if(e.getCode()==404) {
                    	camService.setQueueItemStateTA(items,
                    			ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                    			OffsetDateTime.now(),
                    			e.getMessage());
                    	return true;
                    }
                    // we can retry later
                	camService.setQueueItemStateTA(items,
                			null,
                			OffsetDateTime.now(),
                			e.getMessage());
                	return false;
                } else {
                    // zmenšení velikosti dávky
                    importListSize = 1;
                    return true;
                }                
            } catch (Exception e) {
                // handling other errors -> if it is one record - write the error
                logger.error("Failed to synchronize item(s), list size: {}", items.size(), e);
                // pokud došlo k chybě při čtení 1 záznam najednou
                if (items.size() == 1) {
                    camService.setQueueItemStateTA(items,
                                                   ExtSyncsQueueItem.ExtAsyncQueueState.ERROR, 
                                                   OffsetDateTime.now(),
                                                   e.getMessage());
                    return true;
                } else {
                    // zmenšení velikosti dávky
                    importListSize = 1;                	
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
            if (!upload(item)) {
                return false;
            }
        }
        return true;
    }

    private boolean upload(ExtSyncsQueueItem item) {
        try {
            UploadWorker uploadWorker = camService.prepareUpload(item);
            if (uploadWorker == null) {
                camService.setQueueItemStateTA(Collections.singletonList(item),
                                               ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_OK,
                                               OffsetDateTime.now(),
                                               null);
                return true;
            }
            BatchUpdateResultXml uploadResult = camService.upload(item, uploadWorker.getBatchUpdate());
            if (uploadResult instanceof BatchUpdateSavedXml) {
                BatchUpdateSavedXml savedXml = (BatchUpdateSavedXml) uploadResult;
                uploadWorker.process(camService, savedXml);
            } else {
                // mark as failed
                BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) uploadResult;

                StringBuilder message = new StringBuilder();
                if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                    for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                        message.append(errorMessage.getMsg().getValue()).append("\n");
                    }
                }

                camService.setQueueItemStateTA(Collections.singletonList(item),
                                  ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                                  OffsetDateTime.now(),
                                  message.toString());

            }
        } catch (ApiException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());
            sb.append(", code: ").append(e.getCode());
            String body = e.getResponseBody();
            if (StringUtils.isNotEmpty(body)) {
                sb.append(", response: ").append(body);
            }
            // if ApiException -> it means we connected server and it is logical failure 
            camService.setQueueItemStateTA(Collections.singletonList(item),
                                           ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                                           OffsetDateTime.now(),
                                           sb.toString());
            logger.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), body, e);
            return true;
        } catch (Exception e) {
            logger.error("Failed to synchronize, body: {}", e.getMessage(), e);
            // other exception -> retry later
            camService.setQueueItemStateTA(Collections.singletonList(item),
                                           ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_NEW,
                                           OffsetDateTime.now(),
                                           e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
                // nastavíme velikost dávky pro čtení
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
