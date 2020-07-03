package cz.tacr.elza.ws.core.v1;

import org.springframework.stereotype.Component;

import cz.tacr.elza.ws.types.v1.PermissionsForUser;
import cz.tacr.elza.ws.types.v1.User;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "UserService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.UserService")
public class UserServiceImpl implements UserService {

    @Override
    public void removeUser(String removeUser) throws CoreServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void createUser(User createUser) throws CoreServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPermissions(PermissionsForUser addPermissions) throws CoreServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePermissions(PermissionsForUser removePermissions) throws CoreServiceException {
        // TODO Auto-generated method stub

    }

}
