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
        FilteredResultVO<UsrUserVO> data = findUser(null, true, true, 0, 10);
        Assert.notNull(data);
        Assert.isTrue(data.getTotalCount() == 1);
        Assert.isTrue(data.getList().get(0).getId().equals(user.getId()));

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

        UsrGroupVO sk1 = createGroup(new UserController.CreateGroup(GROUP_NAME, GROUP_CODE));
        Assert.notNull(sk1);
        Assert.notNull(sk1.getId());

        UsrGroupVO sk1change = changeGroup(sk1.getId(), new UserController.ChangeGroup(NAME_GROUP_CHANGe, DESCRIPTION));
        Assert.notNull(sk1change);
        Assert.isTrue(sk1change.getId().equals(sk1.getId()));
        Assert.isTrue(sk1change.getName().equals(NAME_GROUP_CHANGe));
        Assert.isTrue(sk1change.getDescription().equals(DESCRIPTION));

        deleteGroup(sk1.getId());
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
