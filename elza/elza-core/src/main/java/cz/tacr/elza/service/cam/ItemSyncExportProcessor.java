package cz.tacr.elza.service.cam;

import java.time.OffsetDateTime;
import java.util.Collections;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.ErrorMessageXml;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.exception.ExceptionUtils;

@Component
@Scope("prototype")
public class ItemSyncExportProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncExportProcessor.class);

    @Autowired
    private CamService camService;

    private ExtSyncsQueueItem queueItem;

    public ItemSyncExportProcessor(ExtSyncsQueueItem queueItem) {
        this.queueItem = queueItem;
    }

    @Override
    public boolean process() {
        camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                       ExtAsyncQueueState.EXPORT_START,
                                       OffsetDateTime.now(),
                                       null);
        try {
            UploadWorker uploadWorker = camService.prepareUpload(queueItem);
            if (uploadWorker == null) {
                camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                               ExtAsyncQueueState.EXPORT_OK,
                                               OffsetDateTime.now(),
                                               null);
                return true;
            }
            BatchUpdateResultXml uploadResult = camService.upload(queueItem, uploadWorker.getBatchUpdate());
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
                camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                  ExtAsyncQueueState.ERROR,
                                  OffsetDateTime.now(),
                                  message.toString());
            }
        } catch (ApiException e) {
            // if ApiException -> it means we connected server and it is logical failure 
            log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
            camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                           ExtAsyncQueueState.ERROR,
                                           OffsetDateTime.now(),
                                           ExceptionUtils.getApiExceptionInfo(e));
            return true;
        } catch (Exception e) {
            // other exception -> retry later
            log.error("Failed to synchronize: {}", e.getMessage(), e);
            camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                           ExtAsyncQueueState.EXPORT_NEW,
                                           OffsetDateTime.now(),
                                           e.getMessage());
            return false;
        }

        return true;
    }
}
