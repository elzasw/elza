package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.UuidXml;

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

}
