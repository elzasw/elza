package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.cam.v2.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.v2.schema.cam.UuidXml;
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
            values.add(String.valueOf(entityXml.getEntityId().getValue()));
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
            values.add(String.valueOf(entityXml.getEntityUuid().getValue()));
        }
        return values;
    }

    public static String getUuid(UuidXml uuid) {
        return uuid != null ? uuid.getValue() : null;
    }

    public static String getEntityId(EntityXml entityXml) {
        return String.valueOf(entityXml.getEntityId().getValue());
    }

    public static String getEntityUuid(EntityXml entityXml) {
        return entityXml.getEntityUuid().getValue();
    }

    public static String getEntityIdorUuid(ItemEntityRefXml itemEntityRef) {
        EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
        return getEntityIdorUuid(entityRecordRef);
    }

    public static String getEntityIdorUuid(EntityRecordRefXml entityRef) {
        // prepare external ID
        String extIdent = null;
        if (entityRef.getEntityId() != null) {
            extIdent = Long.toString(entityRef.getEntityId().getValue());
        }
        if (extIdent == null && entityRef.getEntityUuid() != null) {
            extIdent = entityRef.getEntityUuid().getValue();
        }
        if (extIdent == null) {
            throw new BusinessException("External ID is empty. UUID or ID has to be provided.", BaseCode.ID_NOT_EXIST);
        }
        return extIdent;
    }
}
