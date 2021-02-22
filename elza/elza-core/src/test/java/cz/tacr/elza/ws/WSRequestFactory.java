package cz.tacr.elza.ws;

import java.util.UUID;

import cz.tacr.elza.ws.types.v1.EntityConflictResolution;
import cz.tacr.elza.ws.types.v1.ImportDisposition;
import cz.tacr.elza.ws.types.v1.ImportRequest;
import cz.tacr.elza.ws.types.v1.ObjectFactory;
import cz.tacr.elza.ws.types.v1.PermissionList;
import cz.tacr.elza.ws.types.v1.User;

/**
 * Helper class to create request
 *
 */
public class WSRequestFactory {

    final static public ObjectFactory objectFactory = new ObjectFactory();

    public static User createUser(String userName, String userId, PermissionList permList) {
        User user = objectFactory.createUser();
        user.setUsername(userName);
        user.setPersonId(userId);
        user.setPermList(permList);
        return user;
    }

    public static ImportRequest createImportRequest(String externalSystemCode, String dataFormat) {
        ImportRequest impRequest = objectFactory.createImportRequest();
        impRequest.setRequestId(UUID.randomUUID().toString());
        impRequest.setExternalSystem(externalSystemCode);
        impRequest.setDataFormat(dataFormat);
        return impRequest;
    }

    public static ImportDisposition createDisposition(String scope) {
        ImportDisposition impDisp = objectFactory.createImportDisposition();
        impDisp.setPrimaryScope(scope);
        impDisp.setConflictResolution(EntityConflictResolution.UPDATE_MODIFIED);
        return impDisp;
    }
}
