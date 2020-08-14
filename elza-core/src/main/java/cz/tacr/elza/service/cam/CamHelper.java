package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * CAM Schema helper methods
 *
 */
public class CamHelper {

    public static String getUuid(UuidXml uuid) {
        if (uuid == null) {
            return null;
        } else {
            return uuid.getValue();
        }
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

}
