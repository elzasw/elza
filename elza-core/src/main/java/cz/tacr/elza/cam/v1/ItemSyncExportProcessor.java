package cz.tacr.elza.cam.v1;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.cam.v1.client.ApiException;
import cz.tacr.cam.v1.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.v1.schema.cam.ErrorMessageXml;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.service.AccessPointConnectorService;

@Component
@Scope("prototype")
public class ItemSyncExportProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncExportProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private AccessPointConnectorService apConnectService;

    private ExtSyncsQueueItem queueItem;

    public ItemSyncExportProcessor(ExtSyncsQueueItem queueItem) {
        this.queueItem = queueItem;
    }

    @Override
    public boolean process() {
		// nahravani lze spustit, jen pokud nebezi synchronizace
		// metoda camService.synchronizeAccessPointsForExternalSystem je nastavena
		// jako synchronized, z toho duvodu je jako zamek zde pouzit stejny
		// objekt
		synchronized (camService) {

			log.debug("Starting upload of queue item, id: {}, accessPointId: {}", queueItem.getExtSyncsQueueItemId(), queueItem.getAccessPointId());

			apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_START);
			try {
				UploadWorker uploadWorker = camService.prepareUpload(queueItem);
				if (uploadWorker == null) {
					apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_OK);
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
					apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.ERROR, message.toString());
				}
			} catch (ApiException e) {
				// if ApiException -> it means we connected server and it is logical failure
				log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
				apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.ERROR, CamException.getApiExceptionInfo(e));
				return true;
			} catch (Exception e) {
				// other exception -> retry later
				log.error("Failed to synchronize: {}", e.getMessage(), e);
				apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_NEW, e.getMessage());
				return false;
			}
			log.debug("Queue item was uploaded, id: {}, accessPointId: {}", queueItem.getExtSyncsQueueItemId(), queueItem.getAccessPointId());
		}

        return true;
    }

    @Override
    public String toString() {
    	return "ItemSyncExportProcessor, queueItem.id: " + queueItem.getExtSyncsQueueItemId() + ", accessPointId: " + queueItem.getAccessPointId();
    }
}
