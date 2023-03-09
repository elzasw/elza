package cz.tacr.elza.service.cam;

import java.time.OffsetDateTime;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;

@Component
@Scope("prototype")
public class ItemSyncUpdateProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncUpdateProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private CamConnector camConnector;

    private ExtSyncsQueueItem queueItem;
    private String bindingValue;

    public ItemSyncUpdateProcessor(ExtSyncsQueueItem queueItem, String bindingValue) {
        this.queueItem = queueItem;
        this.bindingValue = bindingValue;
    }

    @Override
    public boolean process() {
        EntityXml entity;
        try {
            // download entity from CAM
            log.debug("Download entity from CAM, bindingValue: {} externalSystemId: {}", bindingValue, queueItem.getExternalSystemId());
            entity = camConnector.getEntity(bindingValue, queueItem.getExternalSystemId());
        } catch (ApiException e) {
            // if ApiException and code >=400 && <500 =-> it means we connected server and it is logical failure
            if (e.getCode() >= 400 && e.getCode() < 500) {
                camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                  ExtAsyncQueueState.ERROR,
                                  OffsetDateTime.now(),
                                  e.getMessage());
                log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
                return true;
            } else {
                // other exception -> retry later
                camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                  null, // stav se nemění
                                  OffsetDateTime.now(),
                                  e.getMessage());
                return false;
            }
        } catch (Exception e) {
            // other exception -> retry later
            camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                              null, // stav se nemění
                              OffsetDateTime.now(),
                              e.getMessage());
            return false;
        }

        try {
            camService.synchronizeAccessPointTA(entity, queueItem);
        } catch (Exception e) {
            log.error("Failed to synchronize access point, accessPointId: {}", queueItem.getAccessPointId(), e);
            throw e;
        }

        camService.setQueueItemStateTA(Collections.singletonList(queueItem),
                                       ExtSyncsQueueItem.ExtAsyncQueueState.IMPORT_OK,
                                       OffsetDateTime.now(),
                                       "Synchronized: ES -> ELZA");
        return true;
    }
}
