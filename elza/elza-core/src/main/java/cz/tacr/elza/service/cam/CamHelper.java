package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * CAM Schema helper methods
 *
 */
public class CamHelper {

    /**
     * Return list of IDs
     * 
     * @param entities
     * @return
     */
    public static List<String> getEids(List<EntityXml> entities) {
        List<String> values = new ArrayList<>();
        for (EntityXml entityXml : entities) {
            values.add(String.valueOf(entityXml.getEid().getValue()));
        }
        return values;
    }

    /**
     * Return list of UUIDs
     * 
     * @param entities
     * @return
     */
    public static List<String> getEuids(List<EntityXml> entities) {
        List<String> values = new ArrayList<>();
        for (EntityXml entityXml : entities) {
            values.add(String.valueOf(entityXml.getEuid().getValue()));
        }
        return values;
    }

    public static String getUuid(UuidXml uuid) {
        if (uuid == null) {
            return null;
        } else {
            return uuid.getValue();
        }
    }

    public static String getEntityId(EntityXml entityXml) {
        return String.valueOf(entityXml.getEid().getValue());
    }

    public static String getEntityUuid(EntityXml entityXml) {
        return entityXml.getEuid().getValue();
    }

    public static String getEntityIdorUuid(EntityRecordRefXml entityRef) {
        // prepare external ID
        String extIdent = null;
        if (entityRef.getEid() != null) {
            extIdent = Long.toString(entityRef.getEid().getValue());
        }
        if (extIdent == null && entityRef.getEuid() != null) {
            extIdent = entityRef.getEuid().getValue();
        }
        if (extIdent == null) {
            throw new BusinessException("External ID is empty. UUID or ID has to be provided.",
                    BaseCode.ID_NOT_EXIST);
        }

        return extIdent;
    }

    public static String getEntityIdorUuid(ItemEntityRefXml itemEntityRef) {
        EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
        return getEntityIdorUuid(entityRecordRef);
    }

    public static Map<String, EntityXml> getEntitiesByUuid(List<EntityXml> entities) {
        Map<String, EntityXml> uuids = entities.stream()
                .collect(Collectors.toMap(CamHelper::getEntityUuid, Function.identity()));
        return uuids;

    }

}
