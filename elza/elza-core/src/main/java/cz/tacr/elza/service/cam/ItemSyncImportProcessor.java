package cz.tacr.elza.service.cam;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.exception.ExceptionUtils;

@Component
@Scope("prototype")
public class ItemSyncImportProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncImportProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private CamConnector camConnector;

    private List<Integer> queueItemIds = new ArrayList<>();
    private Set<String> bindingValues = new HashSet<>();
    private Integer externalSystemId;

    public ItemSyncImportProcessor(Integer externalSystemId) {
        this.externalSystemId = externalSystemId;
    }

    @Override
    public boolean process() {
        // Special case if we are processing only one record
        List<String> valuesList = new ArrayList<>(bindingValues);
        Map<String, EntityXml> entityXmlMap;

        if (queueItemIds.size() == 1 && valuesList.size() == 1) {
            Integer itemQueueId = queueItemIds.get(0);
            String bindingValue = valuesList.get(0);

            try {
                EntityXml entityXml = camConnector.getEntity(bindingValue, externalSystemId);
                entityXmlMap = Collections.singletonMap(bindingValue, entityXml);
            } catch (ApiException e) {
                // if ApiException -> it means we connected server and it is logical failure 
                log.error("Failed to synchronize item, binding: {}, code: {}, body: {}", bindingValue, e.getCode(), e
                        .getResponseBody(), e);
                // pokud není nalezena žádná položka, přestane se pokoušet o čtení
                if (e.getCode() == 404) {
                    camService.setQueueItemState(itemQueueId,
                            ExtAsyncQueueState.ERROR,
                            OffsetDateTime.now(),
                            ExceptionUtils.getApiExceptionInfo(e));
                    return true;
                }
                // code >=400 && <500 =-> it means we connected server and it is logical failure
                if (e.getCode() >= 400 && e.getCode() < 500) {
                    camService.setQueueItemState(itemQueueId,
                                                 ExtAsyncQueueState.ERROR,
                                                 OffsetDateTime.now(),
                                                 ExceptionUtils.getApiExceptionInfo(e));
                    return true;
                }

                // záznam bude načten znovu
                // TODO: store as last queue state in the queue info
                camService.setQueueItemState(itemQueueId,
                        null, // stav se nemění
                        OffsetDateTime.now(),
                        ExceptionUtils.getApiExceptionInfo(e));
                return false;
            } catch (Exception e) {
                // zkusme si tento záznam přečíst znovu
                // TODO: store as last queue state in the queue info
                camService.setQueueItemState(itemQueueId,
                                             null, // stav se nemění
                                             OffsetDateTime.now(),
                                             e.getMessage());
                return false;
            }

        } else {
            try {
                EntitiesXml entitiesXml = camConnector.getEntities(valuesList, externalSystemId);

                // entitities XML to map
                entityXmlMap = entitiesXml.getList().stream()
                        .collect(Collectors.toMap(e -> String.valueOf(e.getEid().getValue()), Function.identity()));

            } catch (ApiException e) {
                // if ApiException -> it means we connected server and it is logical failure 
                log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                // handling other errors -> if it is one record - write the error
                log.error("Failed to synchronize item(s), list size: {}", queueItemIds.size(), e);
                throw e;
            }
        }

        try {
            camService.importEntities(externalSystemId, entityXmlMap, queueItemIds);
            log.info("Downloaded {} entity from CAM", queueItemIds.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to synchronize access points: {}", queueItemIds.size(), e);
            throw e;
        }

    }

    public void addQueueItem(ExtSyncsQueueItem queueItem) {
        queueItemIds.add(queueItem.getExtSyncsQueueItemId());
    }

    public void addBindingValue(String value) {
        bindingValues.add(value);
    }
}