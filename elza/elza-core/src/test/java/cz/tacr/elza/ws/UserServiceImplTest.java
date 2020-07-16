package cz.tacr.elza.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.jayway.restassured.RestAssured;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.ws.core.v1.UserService;
import cz.tacr.elza.ws.types.v1.ObjectFactory;
import cz.tacr.elza.ws.types.v1.Permission;
import cz.tacr.elza.ws.types.v1.PermissionList;
import cz.tacr.elza.ws.types.v1.PermissionType;
import cz.tacr.elza.ws.types.v1.PermissionsForUser;
import cz.tacr.elza.ws.types.v1.SetUserState;
import cz.tacr.elza.ws.types.v1.User;
import cz.tacr.elza.ws.types.v1.UserState;

public class UserServiceImplTest extends AbstractControllerTest {

    ObjectFactory objectFactory = new ObjectFactory();

    @Test
    public void userServiceTest() {
        List<ApAccessPointVO> aps = this.findRecord(null, 0, 1, null, null, null);
        assertTrue(aps.size() > 0);
        Integer apId = aps.get(0).getId();

        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.USER_SERVICE_URL;
        UserService userServiceClient = DaoServiceClientFactory.createUserService(address, "admin", "admin");
        User createUser = objectFactory.createUser();
        createUser.setUsername("a");
        createUser.setPersonId(apId.toString());
        PermissionList createUserPerms = objectFactory.createPermissionList();
        Permission createUserPermReadAll = objectFactory.createPermission();
        createUserPermReadAll.setPermType(PermissionType.FUND_RD_ALL);
        createUserPerms.getPerm().add(createUserPermReadAll);
        createUser.setPermList(createUserPerms);
        userServiceClient.createUser(createUser);
        
        // validate user exists
        FilteredResultVO<UsrUserVO> usersFound = this.findUser(null, true, false, 0, 100, null);
        assertTrue(usersFound.getCount() == 1);
        UsrUserVO userInfo = usersFound.getRows().get(0);
        Integer userId = userInfo.getId();
        assertNotNull(userId);

        // add permissions
        PermissionsForUser addPermissions = objectFactory.createPermissionsForUser();
        addPermissions.setUsername("a");
        PermissionList addPerms = objectFactory.createPermissionList();
        Permission userPermWriteAll = objectFactory.createPermission();
        userPermWriteAll.setPermType(PermissionType.FUND_ARR_ALL);
        addPerms.getPerm().add(userPermWriteAll);
        addPermissions.setPermList(addPerms);
        userServiceClient.addPermissions(addPermissions);

        userInfo = getUser(userId);
        assertTrue(userInfo.getPermissions().size() == 2);

        // remove permissions
        PermissionsForUser removePermissions = objectFactory.createPermissionsForUser();
        removePermissions.setUsername("a");
        PermissionList removePerms = objectFactory.createPermissionList();
        removePerms.getPerm().add(userPermWriteAll);
        removePermissions.setPermList(removePerms);
        userServiceClient.removePermissions(removePermissions);

        userInfo = getUser(userId);
        assertTrue(userInfo.getPermissions().size() == 1);
        UsrPermissionVO userInfoPerm = userInfo.getPermissions().get(0);
        assertEquals(userInfoPerm.getPermission().name(), "FUND_RD_ALL");

        // deactivate user
        SetUserState setUserState = objectFactory.createSetUserState();
        setUserState.setUsername("a");
        setUserState.setState(UserState.INACTIVE);
        userServiceClient.setUserState(setUserState);
    }
}
