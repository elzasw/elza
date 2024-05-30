package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrFundBaseVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.AdminCopyPermissionParams;
import cz.tacr.elza.test.controller.vo.Fund;


/**
 * Testy pro uživatelskou sekci.
 *
 */
public class UserControllerTest extends AbstractControllerTest {

    public static final String USER = "user1";
    public static final String USER2 = "user2";
    public static final String PASS = "pass";
    public static final String PASS_NEW = "pass_new";
    public static final String NAME_GROUP_CHANGE = "Skupina 1x";
    public static final String DESCRIPTION = "popis";
    public static final String GROUP_NAME = "Skupina 1";
    public static final String GROUP_CODE = "SK1";

    @Test
    public void userDetail() {
        UserInfoVO userDetail = getUserDetail();
		assertNotNull(userDetail);
		assertNotNull(userDetail.getUsername());
		assertTrue(userDetail.getUserPermissions().size() > 0);
    }

    @Test
    public void usersTest() throws ApiException {

        Fund fund = createFund("Test", "TST");

        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        ApAccessPointVO ap = records.get(0);

        // vytvoření uživatele
        UsrUserVO user = createUser(ap, USER, PASS);

        // vyhledání uživatele
        FilteredResultVO<UsrUserVO> dataUser = findUser(null, true, true, 0, 10, null);
		assertNotNull(dataUser);
        assertEquals(1, dataUser.getCount());
        assertEquals(dataUser.getRows().get(0).getId(), user.getId());

        UsrUserVO userReturn = getUser(user.getId());
		assertNotNull(userReturn);
        assertEquals(user.getId(), userReturn.getId());

        // změna hesla - administrace
        changePassword(user, PASS_NEW);

        // změna hesla - uživatel
        try {
            changePassword(PASS_NEW, PASS);
        } catch (AssertionError e) {
            // je ok, protože je přihlášen admin - virtuální uživatel, který není v DB
        }

        // aktivace/deaktivace uživatele
        changeActive(user);

        UsrGroupVO group = createGroup(new GroupController.CreateGroup(GROUP_NAME, GROUP_CODE));
		assertNotNull(group);
		assertNotNull(group.getId());

        UsrGroupVO groupChange = changeGroup(group.getId(), new GroupController.ChangeGroup(NAME_GROUP_CHANGE, DESCRIPTION));
		assertNotNull(groupChange);
        assertEquals(groupChange.getId(), group.getId());
        assertEquals(NAME_GROUP_CHANGE, groupChange.getName());
        assertEquals(DESCRIPTION, groupChange.getDescription());

        // vyhledání skupiny
        FilteredResultVO<UsrGroupVO> dataGroup = findGroup(null, 0, 10);
		assertNotNull(dataGroup);
        assertEquals(1, dataGroup.getCount());
        assertEquals(dataGroup.getRows().get(0).getId(), group.getId());

        Set<Integer> groupIds = new HashSet<>();
        groupIds.add(group.getId());
        Set<Integer> userIds = new HashSet<>();
        userIds.add(user.getId());

        //Kontrola počtu uživatelů před přidáním
        FilteredResultVO<UsrUserVO> findUsers = findUser(null, true, false, 0, 10, group.getId());
        assertEquals(1, findUsers.getCount());

        joinGroup(groupIds, userIds);

        //Konstrola počtu uživatelů s vynechanou skupinou po přidání do skupiny
        findUsers = findUser(null, true, false, 0, 10, group.getId());
        assertEquals(0, findUsers.getCount());

        group = getGroup(group.getId());
        assertEquals(1, group.getUsers().size());

        user = getUser(user.getId());
        assertEquals(1, user.getGroups().size());

        UsrGroupVO groupReturn = getGroup(group.getId());
		assertNotNull(groupReturn);
        assertEquals(groupReturn.getId(), group.getId());

        // ##
        // # Oprávnění
        // ##
        List<UsrPermissionVO> permissionVOs = new ArrayList<>();

        UsrPermissionVO permissionVO = new UsrPermissionVO();
        permissionVO.setPermission(UsrPermission.Permission.FUND_RD_ALL);
        permissionVOs.add(permissionVO);

        permissionVO = new UsrPermissionVO();
        permissionVO.setFund(ArrFundBaseVO.newInstance(fund.getId(), fund.getName(), fund.getInternalCode(),
                                                       fund.getFundNumber(), fund.getMark()));
        permissionVO.setPermission(UsrPermission.Permission.FUND_ARR);
        permissionVOs.add(permissionVO);

        permissionVO = new UsrPermissionVO();
        ApScopeVO apScopeVO = new ApScopeVO();
        apScopeVO.setId(1);
        permissionVO.setScope(apScopeVO);
        permissionVO.setPermission(UsrPermission.Permission.AP_SCOPE_RD);
        permissionVOs.add(permissionVO);

        // Přidání a odebrání oprávnění na uživatele
        addUserPermission(user.getId(), permissionVOs);
        user = getUser(user.getId());
		assertNotNull(user.getPermissions());
        assertEquals(3, user.getPermissions().size());

        for (UsrPermissionVO usrPermissionVO : user.getPermissions()) {
            deleteUserPermission(user.getId(), usrPermissionVO);
        }
        user = getUser(user.getId());
		assertNotNull(user.getPermissions());
		assertTrue(user.getPermissions().size() == 0);

        // Přidání a odebrání oprávnění na skupinu
        addGroupPermission(group.getId(), permissionVOs);
        group = getGroup(group.getId());
		assertNotNull(group.getPermissions());
		assertTrue(group.getPermissions().size() == 3);

        for (UsrPermissionVO x : group.getPermissions()) {
            deleteGroupPermission(group.getId(), x);
        }
        group = getGroup(group.getId());
		assertNotNull(group.getPermissions());
		assertTrue(group.getPermissions().size() == 0);

        // ##
        // # Oprávnění na AS all a na fund
        // ##

        permissionVOs.clear();

        permissionVO = new UsrPermissionVO();
        permissionVO.setFund(ArrFundBaseVO.newInstance(fund.getId(), fund.getName(), fund.getInternalCode(),
                                                       fund.getFundNumber(), fund.getMark()));
        permissionVO.setPermission(UsrPermission.Permission.FUND_RD);
        permissionVOs.add(permissionVO);

        permissionVO = new UsrPermissionVO();
        permissionVO.setPermission(UsrPermission.Permission.FUND_RD_ALL);
        permissionVOs.add(permissionVO);

        // Uživatel
        addUserPermission(user.getId(), permissionVOs);
		assertTrue(getUser(user.getId()).getPermissions().size() == 2);
        deleteUserFundPermission(user.getId(), fund.getId());
		assertTrue(getUser(user.getId()).getPermissions().size() == 1);
        deleteUserFundAllPermission(user.getId());
		assertTrue(getUser(user.getId()).getPermissions().size() == 0);

        // Skupina
        addGroupPermission(group.getId(), permissionVOs);
		assertTrue(getGroup(group.getId()).getPermissions().size() == 2);
        deleteGroupFundPermission(group.getId(), fund.getId());
		assertTrue(getGroup(group.getId()).getPermissions().size() == 1);
        deleteGroupFundAllPermission(group.getId());
		assertTrue(getGroup(group.getId()).getPermissions().size() == 0);

        // ##
        // ---
        leaveGroup(group.getId(), user.getId());

        deleteGroup(group.getId());
    }

    /**
     * Zablokování/aktivace uživatele.
     *
     * @param user uživatel
     */
    private void changeActive(final UsrUserVO user) {
        UsrUserVO user2 = changeActive(user, false);
        assertFalse(user2.isActive());

        user2 = changeActive(user, true);
        assertTrue(user2.isActive());
    }

    /**
     * Vytvoření uživatele.
     *
     * @return vytvořený uživatel
     * @param ap
     */
    private UsrUserVO createUser(final ApAccessPointVO ap, String userName, String password) {
        Map<UsrAuthentication.AuthType, String> valueMap = new HashMap<>();
        valueMap.put(UsrAuthentication.AuthType.PASSWORD, password);
        UsrUserVO user = createUser(userName, valueMap, ap.getId());
		assertNotNull(user);
		assertNotNull(user.getId());
        return user;
    }

    @Test
    public void copyPermissionsTest() throws ApiException {
        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        ApAccessPointVO ap = records.get(0);

        // vytvoření uživatele
        UsrUserVO user = createUser(ap, USER, PASS);

        List<UsrPermissionVO> permissionVOs = new ArrayList<>();
        UsrPermissionVO permissionVO = new UsrPermissionVO();
        permissionVO.setPermission(UsrPermission.Permission.FUND_RD_ALL);
        permissionVOs.add(permissionVO);
        addUserPermission(user.getId(), permissionVOs);

        // pridani do skupiny
        UsrGroupVO group = createGroup(new GroupController.CreateGroup(GROUP_NAME, GROUP_CODE));
        assertNotNull(group);
        assertNotNull(group.getId());
        joinGroup(Collections.singleton(group.getId()), Collections.singleton(user.getId()));

        // vytvoreni druheho uzivatele
        UsrUserVO user2 = createUser(ap, USER2, PASS);
        // copy
        AdminCopyPermissionParams cpp = new AdminCopyPermissionParams();
        cpp.setFromUserId(user.getId());
        adminApi.adminCopyPermissions(user2.getId(), cpp);

        // read permissions for user2
        UsrUserVO usr2Vo = getUser(user2.getId());
        List<UsrGroupVO> groups = usr2Vo.getGroups();
        assertEquals(groups.size(), 1);
        List<UsrPermissionVO> permList = usr2Vo.getPermissions();
        assertEquals(permList.size(), 1);

    }
}
