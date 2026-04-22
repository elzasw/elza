package cz.tacr.elza.cam.v2;

import java.util.UUID;

import javax.xml.validation.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.cam.v2.client.ApiException;
import cz.tacr.cam.v2.schema.cam.BatchUpdateXml;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.cam.JaxbUtils;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.service.AccessPointConnectorService;

@Component("ItemSyncExportProcessorV2")
@Scope("prototype")
public class ItemSyncExportProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncExportProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private SchemaManager schemaManager;

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
				BatchUpdateXml batchUpdate = uploadWorker.getBatchUpdate();
				Schema schema = schemaManager.getSchema(SchemaManager.CAM_SCHEMA_2025);
		        String batchUpdateString = JaxbUtils.asString(batchUpdate, schema);
		        String batchUpdateInfoUuid = batchUpdate.getInfo().getUuid().getValue();
				UUID uuidResponse = camService.upload(queueItem, batchUpdateString, batchUpdateInfoUuid);
				// Binding is intentionally NOT created here. It is created only after CAM confirms
				// the batch was stored (see ItemSyncExportConfirmProcessor). Creating it eagerly
				// would leave an orphan binding in ELZA when CAM rejects/revokes the batch.
				// Persist the new-part / new-item uuid map so the confirm processor can resolve
				// IssueXml.partRef/itemRef back to ELZA ids when CAM returns warnings.
				String uuidMapJson = UuidMapping.serialize(uploadWorker.getPartUuidMap(), uploadWorker.getItemUuidMap());
				apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_PROCESSING, null, uuidResponse.toString(), batchUpdateString, null, uuidMapJson);

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
