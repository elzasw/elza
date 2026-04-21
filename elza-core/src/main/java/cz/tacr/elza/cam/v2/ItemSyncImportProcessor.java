package cz.tacr.elza.cam.v2;

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

import cz.tacr.cam.v2.client.ApiException;
import cz.tacr.cam.v2.schema.cam.EntitiesXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.service.AccessPointConnectorService;

/**
 * Item import processor over CAM v2 protocol.
 *
 * Downloads entities from CAM v2 and imports them into ELZA.
 */
@Component("ItemSyncImportProcessorV2")
@Scope("prototype")
public class ItemSyncImportProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncImportProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private CamConnector camConnector;

    @Autowired
    private AccessPointConnectorService apConnectService;

    @Autowired
    private StaticDataService staticDataService;

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
                // ApiException -> server was reached and returned a logical failure
                log.error("Failed to synchronize item, binding: {}, code: {}, body: {}", bindingValue, e.getCode(), e
                        .getResponseBody(), e);
                // entity not found -> stop retrying
                if (e.getCode() == 404) {
                    apConnectService.setQueueItemStateTA(itemQueueId,
                                                         ExtAsyncQueueState.ERROR,
                                                         CamException.getApiExceptionInfo(e));
                    return true;
                }
                // 4xx -> server was reached, logical failure, stop retrying
                if (e.getCode() >= 400 && e.getCode() < 500) {
                    apConnectService.setQueueItemStateTA(itemQueueId,
                                                         ExtAsyncQueueState.ERROR,
                                                         CamException.getApiExceptionInfo(e));
                    return true;
                }

                // leave the item for another retry
                // TODO: store as last queue state in the queue info
                apConnectService.setQueueItemStateTA(itemQueueId,
                                                     null,
                                                     CamException.getApiExceptionInfo(e));
                return false;
            } catch (Exception e) {
                // other errors -> leave the item for another retry
                // TODO: store as last queue state in the queue info
                apConnectService.setQueueItemStateTA(itemQueueId,
                                                     null,
                                                     e.getMessage());
                return false;
            }

        } else {
            try {
                EntitiesXml entitiesXml = camConnector.getEntities(valuesList, externalSystemId);

                // entities XML to map, keyed consistently with the binding values produced
                // by prepareApsForSync: UUID for CAM_UUID variants, entity ID otherwise
                ApExternalSystem externalSystem = staticDataService.getData().getApExternalSystemById(externalSystemId);
                Function<EntityXml, String> keyFn =
                        externalSystem.getType().getBaseType() == ApExternalSystemType.CAM_UUID_V2
                                ? CamHelper::getEntityUuid
                                : CamHelper::getEntityId;
                entityXmlMap = entitiesXml.getEntity().stream()
                        .collect(Collectors.toMap(keyFn, Function.identity()));

            } catch (ApiException e) {
                // ApiException -> server was reached and returned a logical failure
                log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                // other errors -> if it is a single record the error was already handled above
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

    @Override
    public String toString() {
        return "ItemSyncImportProcessor(v2), externalSystemId: " + externalSystemId + ", queueItemIds: " + queueItemIds;
    }
}
