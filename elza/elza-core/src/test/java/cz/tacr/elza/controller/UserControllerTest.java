package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.*;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.UsrAuthentication;
import org.junit.Test;

import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.domain.UsrPermission;


/**
 * Testy pro uživatelskou sekci.
 *
 */
public class UserControllerTest extends AbstractControllerTest {

    public static final String USER = "user1";
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
    public void usersTest() {

        ArrFundVO fund = createFund("Test", "TST");

        // vytvoření uživatele
        UsrUserVO user = createUser();

        // vyhledání uživatele
        FilteredResultVO<UsrUserVO> dataUser = findUser(null, true, true, 0, 10, null);
		assertNotNull(dataUser);
		assertTrue(dataUser.getCount() == 1);
		assertTrue(dataUser.getRows().get(0).getId().equals(user.getId()));

        UsrUserVO userReturn = getUser(user.getId());
		assertNotNull(userReturn);
		assertTrue(userReturn.getId().equals(user.getId()));

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
		assertTrue(groupChange.getId().equals(group.getId()));
		assertTrue(groupChange.getName().equals(NAME_GROUP_CHANGE));
		assertTrue(groupChange.getDescription().equals(DESCRIPTION));

        // vyhledání skupiny
        FilteredResultVO<UsrGroupVO> dataGroup = findGroup(null, 0, 10);
		assertNotNull(dataGroup);
		assertTrue(dataGroup.getCount() == 1);
		assertTrue(dataGroup.getRows().get(0).getId().equals(group.getId()));

        Set<Integer> groupIds = new HashSet<>();
        groupIds.add(group.getId());
        Set<Integer> userIds = new HashSet<>();
        userIds.add(user.getId());

        //Kontrola počtu uživatelů před přidáním
        FilteredResultVO<UsrUserVO> findUsers = findUser(null, true, false, 0, 10, group.getId());
		assertTrue(findUsers.getCount() == 1);

        joinGroup(groupIds, userIds);

        //Konstrola počtu uživatelů s vynechanou skupinou po přidání do skupiny
        findUsers = findUser(null, true, false, 0, 10, group.getId());
		assertTrue(findUsers.getCount() == 0);

        group = getGroup(group.getId());
		assertTrue(group.getUsers().size() == 1);

        user = getUser(user.getId());
		assertTrue(user.getGroups().size() == 1);

        UsrGroupVO groupReturn = getGroup(group.getId());
		assertNotNull(groupReturn);
		assertTrue(groupReturn.getId().equals(group.getId()));

        // ##
        // # Oprávnění
        // ##
        List<UsrPermissionVO> permissionVOs = new ArrayList<>();

        UsrPermissionVO permissionVO = new UsrPermissionVO();
        permissionVO.setPermission(UsrPermission.Permission.FUND_RD_ALL);
        permissionVOs.add(permissionVO);

        permissionVO = new UsrPermissionVO();
        permissionVO.setFund(fund);
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
		assertTrue(user.getPermissions().size() == 3);

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
        permissionVO.setFund(fund);
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
		assertTrue(user2.isActive() == false);

        user2 = changeActive(user, true);
		assertTrue(user2.isActive() == true);
    }

    /**
     * Vytvoření uživatele.
     *
     * @return vytvořený uživatel
     */
    private UsrUserVO createUser() {
        List<ParPartyVO> party = findParty(null, 0, 1, null, null);

        Map<UsrAuthentication.AuthType, String> valueMap = new HashMap<>();
        valueMap.put(UsrAuthentication.AuthType.PASSWORD, PASS);
        UsrUserVO user = createUser(USER, valueMap, party.get(0).getId());
		assertNotNull(user);
		assertNotNull(user.getId());
        return user;
    }

}
