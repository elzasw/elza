package cz.tacr.elza.service.cam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.service.ExtSyncsProcessor;

@Component
@Scope("prototype")
public class ItemSyncImportNewProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncImportNewProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private CamConnector camConnector;

    private List<ExtSyncsQueueItem> queueItems;
    private List<String> bindingValues;
    private Map<String, ApBinding> bindingMap;
    private Integer externalSystemId;

    public ItemSyncImportNewProcessor(List<ExtSyncsQueueItem> queueItems, List<String> bindingValues, Map<String, ApBinding> bindingMap, Integer externalSystemId) {
        this.queueItems = queueItems;
        this.bindingValues = bindingValues;
        this.bindingMap = bindingMap;
        this.externalSystemId = externalSystemId;
    }

    @Override
    public boolean process() {
        EntitiesXml entities;
        try {
            entities = camConnector.getEntities(bindingValues, externalSystemId);
        } catch (ApiException e) {
            // if ApiException -> it means we connected server and it is logical failure 
            log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
            // pokud došlo k chybě při čtení 1 záznam najednou
            if (queueItems.size() == 1) {
                // pokud není nalezena žádná položka, přestane se pokoušet o čtení
                if (e.getCode() == 404) {
                    camService.setQueueItemStateTA(queueItems,
                            ExtAsyncQueueState.ERROR,
                            OffsetDateTime.now(),
                            ExceptionUtils.getApiExceptionInfo(e));
                    return true;
                }
                // zkusme si tento záznam přečíst znovu
                camService.setQueueItemStateTA(queueItems,
                        null, // stav se nemění
                        OffsetDateTime.now(),
                        ExceptionUtils.getApiExceptionInfo(e));
                return false;
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            // handling other errors -> if it is one record - write the error
            log.error("Failed to synchronize item(s), list size: {}", queueItems.size(), e);
            // pokud došlo k chybě při čtení 1 záznam najednou
            if (queueItems.size() == 1) {
                // zkusme si tento záznam přečíst znovu
                camService.setQueueItemStateTA(queueItems,
                                               null, // stav se nemění
                                               OffsetDateTime.now(),
                                               e.getMessage());
                return false;
            }
            throw e;
        }

        try {
            camService.importNew(externalSystemId, entities, bindingMap);
            log.info("Download {} entity from CAM", queueItems.size());
        } catch (Exception e) {
            log.error("Failed to synchronize access points: {}", queueItems.size(), e);
            throw e;
        }

        camService.setQueueItemStateTA(queueItems,
                                       ExtAsyncQueueState.IMPORT_OK,
                                       OffsetDateTime.now(),
                                       "Synchronized: ES -> ELZA");
        return true;
    }
}