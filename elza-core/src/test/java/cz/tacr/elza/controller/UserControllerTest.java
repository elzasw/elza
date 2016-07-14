package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;


/**
 * Testy pro uživatelskou sekci.
 *
 * @author Martin Šlapa
 * @since 11.05.2016
 */
public class UserControllerTest extends AbstractControllerTest {

    public static final String USER = "user1";
    public static final String PASS = "pass";
    public static final String PASS_NEW = "pass_new";
    public static final String NAME_GROUP_CHANGe = "Skupina 1x";
    public static final String DESCRIPTION = "popis";
    public static final String GROUP_NAME = "Skupina 1";
    public static final String GROUP_CODE = "SK1";

    @Test
    public void userDetail() {
        UserInfoVO userDetail = getUserDetail();
        Assert.notNull(userDetail);
        Assert.notNull(userDetail.getUsername());
        Assert.notEmpty(userDetail.getUserPermissions());
    }

    @Test
    public void usersTest() {

        // vytvoření uživatele
        UsrUserVO user = createUser();

        // vyhledání uživatele
        FilteredResultVO<UsrUserVO> dataUser = findUser(null, true, true, 0, 10);
        Assert.notNull(dataUser);
        Assert.isTrue(dataUser.getTotalCount() == 1);
        Assert.isTrue(dataUser.getList().get(0).getId().equals(user.getId()));

        UsrUserVO userReturn = getUser(user.getId());
        Assert.notNull(userReturn);
        Assert.isTrue(userReturn.getId().equals(user.getId()));

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

        UsrGroupVO group = createGroup(new UserController.CreateGroup(GROUP_NAME, GROUP_CODE));
        Assert.notNull(group);
        Assert.notNull(group.getId());

        UsrGroupVO groupChange = changeGroup(group.getId(), new UserController.ChangeGroup(NAME_GROUP_CHANGe, DESCRIPTION));
        Assert.notNull(groupChange);
        Assert.isTrue(groupChange.getId().equals(group.getId()));
        Assert.isTrue(groupChange.getName().equals(NAME_GROUP_CHANGe));
        Assert.isTrue(groupChange.getDescription().equals(DESCRIPTION));

        // vyhledání skupiny
        FilteredResultVO<UsrGroupVO> dataGroup = findGroup(null, 0, 10);
        Assert.notNull(dataGroup);
        Assert.isTrue(dataGroup.getTotalCount() == 1);
        Assert.isTrue(dataGroup.getList().get(0).getId().equals(group.getId()));

        joinGroup(group.getId(), user.getId());

        group = getGroup(group.getId());
        Assert.isTrue(group.getUsers().size() == 1);

        user = getUser(user.getId());
        Assert.isTrue(user.getGroups().size() == 1);

        UsrGroupVO groupReturn = getGroup(group.getId());
        Assert.notNull(groupReturn);
        Assert.isTrue(groupReturn.getId().equals(group.getId()));

        deleteGroup(group.getId());
    }

    /**
     * Zablokování/aktivace uživatele.
     *
     * @param user uživatel
     */
    private void changeActive(final UsrUserVO user) {
        UsrUserVO user2 = changeActive(user, false);
        Assert.isTrue(user2.isActive() == false);

        user2 = changeActive(user, true);
        Assert.isTrue(user2.isActive() == true);
    }

    /**
     * Vytvoření uživatele.
     *
     * @return vytvořený uživatel
     */
    private UsrUserVO createUser() {
        List<ParPartyVO> party = findParty(null, 0, 1, null, null);

        UsrUserVO user = createUser(USER, PASS, party.get(0).getPartyId());
        Assert.notNull(user);
        Assert.notNull(user.getId());
        return user;
    }

}
