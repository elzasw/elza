package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrFundBaseVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UISettingsVO;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.UserService;

/**
 * Kontroler pro uživatele.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ClientFactoryVO factoryVO;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ArrangementController arrangementController;

    @Autowired
    private FundRepository fundRepository;

    /**
     * Získání oprávnění uživatele.
     *
     * @return výčet oprávnění uživatele.
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
	@Transactional
    public UserInfoVO getUserDetail() {
        final UserDetail userDetail = userService.getLoggedUserDetail();
        return factoryVO.createUserInfo(userDetail);
    }

    /**
     * Uložit nastavení uživatele.
     *
     * @param settings Seznam nastavení uživatele.
     */
    @RequestMapping(value = "/detail/settings", method = RequestMethod.PUT)
	@Transactional
    public List<UISettingsVO> setUserSettings(@RequestBody final List<UISettingsVO> settings) {
        List<UISettings> settingsList = factoryDO.createSettingsList(settings);

        UsrUser user = userService.getLoggedUser();
        if (user != null && settingsList.size() > 0) {
            settingsService.setSettings(user, settingsList);
        } else {
            int i = 1;
            for (UISettings uiSettings : settingsList) {
                uiSettings.setSettingsId(i++);
            }
        }

        return factoryVO.createSettingsList(settingsList);
    }

    /**
     * Vytvořené nového uživatele.
     *
     * @param params parametry pro vytvoření uživatele
     * @return vytvořený uživatel
     */
    @RequestMapping(method = RequestMethod.POST)
    @Transactional
    public UsrUserVO createUser(@RequestBody final CreateUser params) {
        Assert.notNull(params, "Parametry musí být vyplněny");

        UsrUser user = userService.createUser(params.getUsername(), params.getPassword(), params.getPartyId());
        return factoryVO.createUser(user, true, true);
    }

    /**
     * Upravení uživatele.
     *
     * @param params parametry pro úpravu uživatele
     * @return upravený uživatel
     */
    @RequestMapping(value = "/{userId}",method = RequestMethod.PUT)
    @Transactional
    public UsrUserVO changeUser(@PathVariable("userId") final Integer userId,
                                @RequestBody final CreateUser params) {
        Assert.notNull(params, "Parametry musí být vyplněny");

        UsrUser user = userService.getUser(userId);

        if (user == null) {
            throw new ObjectNotFoundException("Uživatel neexistuje", UserCode.USER_NOT_FOUND).set("id", userId);
        }

        user = userService.changeUser(user, params.getUsername(), params.getPassword());
        return factoryVO.createUser(user, true, true);
    }

    /**
     * Změna hesla uživatele - administrace.
     *
     * @param userId uživate, kterému měníme heslo
     * @param params parametry změny hesla
     * @return upravený uživatel
     */
    @RequestMapping(value = "/{userId}/password", method = RequestMethod.PUT)
    @Transactional
    public UsrUserVO changePassword(@PathVariable("userId") final Integer userId,
                                    @RequestBody final ChangePassword params) {
        Assert.notNull(params, "Parametry musí být vyplněny");

        UsrUser user = userService.getUser(userId);

        if (user == null) {
            throw new ObjectNotFoundException("Uživatel neexistuje", UserCode.USER_NOT_FOUND).set("id", userId);
        }

        user = userService.changePassword(user, params.getNewPassword());
        return factoryVO.createUser(user, true, true);
    }

    /**
     * Změna hesla uživatele - uživatel.
     *
     * @param params parametry změny hesla
     * @return upravený uživatel
     */
    @RequestMapping(value = "/password", method = RequestMethod.PUT)
    @Transactional
    public UsrUserVO changePassword(@RequestBody final ChangePassword params) {
        Assert.notNull(params, "Parametry musí být vyplněny");

        UsrUser user = userService.getLoggedUser();

        if (user == null) {
            throw new SystemException("Uživatel není přihlášen", UserCode.USER_NOT_LOGGED);
        }

        if (StringUtils.isEmpty(params.getOldPassword())) {
            throw new BusinessException("Je nutné zadat původní heslo", BaseCode.PROPERTY_NOT_EXIST).set("property", "oldPassword");
        }

        if (StringUtils.isEmpty(params.getNewPassword())) {
            throw new BusinessException("Je nutné zadat nové heslo", BaseCode.PROPERTY_NOT_EXIST).set("property", "newPassword");
        }

        user = userService.changePassword(user, params.getOldPassword(), params.getNewPassword());
        return factoryVO.createUser(user, true, true);
    }

    /**
     * Změna aktivace/deaktivace uživatele.
     *
     * @param userId uživate, kterému měníme heslo
     * @param active aktivace/blokace
     * @return upravený uživatel
     */
    @RequestMapping(value = "/{userId}/active/{active}", method = RequestMethod.PUT)
    @Transactional
    public UsrUserVO changeActive(@PathVariable("userId") final Integer userId,
                                  @PathVariable("active") final Boolean active) {

        UsrUser user = userService.getUser(userId);

        if (user == null) {
            throw new IllegalArgumentException("Uživatel neexistuje");
        }

        user = userService.changeActive(user, active);
        return factoryVO.createUser(user, true, true);
    }

    /**
     * Načtení uživatele s daty pro zobrazení na detailu s možností editace.
     *
     * @param userId id
     * @return VO
     */
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
	@Transactional
    public UsrUserVO getUser(@PathVariable(value = "userId") final Integer userId) {
        Assert.notNull(userId, "Identifikátor uživatele musí být vyplněno");

        UsrUser user = userService.getUser(userId);
        return factoryVO.createUser(user, true, true);
    }

    /**
     * Načte seznam uživatelů.
     *
     * @param search   hledaný řetězec
     * @param from     počáteční záznam
     * @param count    počet vrácených záznamů
     * @param active   mají se vracet aktivní osoby?
     * @param disabled mají se vracet zakázané osoby?
     * @return seznam s celkovým počtem
     */
    @RequestMapping(method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<UsrUserVO> findUser(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                @RequestParam("active") final Boolean active,
                                                @RequestParam("disabled") final Boolean disabled,
                                                @RequestParam("from") final Integer from,
                                                @RequestParam("count") final Integer count,
                                                @RequestParam(value = "excludedGroupId", required = false) final Integer excludedGroupId
    ) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        FilteredResult<UsrUser> users = userService.findUser(search, active, disabled, from, count, excludedGroupId);
        List<UsrUserVO> resultVo = factoryVO.createUserList(users.getList(), false);
        return new FilteredResultVO<>(resultVo, users.getTotalCount());
    }

    /**
     * Načte seznam uživatelů, kteří mají přiřazené nebo zděděné oprávnění na zakládání nových AS.
     *
     * @param search   hledaný řetězec
     * @param from     počáteční záznam
     * @param count    počet vrácených záznamů
     * @param active   mají se vracet aktivní osoby?
     * @param disabled mají se vracet zakázané osoby?
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "/withFundCreate", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<UsrUserVO> findUserWithFundCreate(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                @RequestParam("active") final Boolean active,
                                                @RequestParam("disabled") final Boolean disabled,
                                                @RequestParam("from") final Integer from,
                                                @RequestParam("count") final Integer count,
                                                @RequestParam(value = "excludedGroupId", required = false) final Integer excludedGroupId
    ) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        FilteredResult<UsrUser> users = userService.findUserWithFundCreate(search, active, disabled, from, count, excludedGroupId);
        List<UsrUserVO> resultVo = factoryVO.createUserList(users.getList(), false);
        return new FilteredResultVO<>(resultVo, users.getTotalCount());
    }

    /**
     * Načtení seznamu archivních souborů, pro které může aktuální uživatel nastavovat oprávnění.
     *
     * @param search hledací výraz
     * @param from počáteční záznam
     * @param count počet vrácených záznamů
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "/controlFunds", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<ArrFundBaseVO> findControlFunds(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                            @RequestParam("from") final Integer from,
                                                            @RequestParam("count") final Integer count
    ) {
        FilteredResult<ArrFund> funds = userService.findFundsWithPermissions(search, from, count);
        return new FilteredResultVO<>(factoryVO.createSimpleEntity(funds.getList(), ArrFundBaseVO.class), funds.getTotalCount());
    }

    /**
     * Načte seznam uživatelů, kteří mají explicitně (přímo na nich) nastavené nějaké oprávnění pro daný AS.
     * @param fundId id AS
     * @return seznam
     */
    @RequestMapping(value = "/fund/{fundId}/users", method = RequestMethod.GET)
	@Transactional
    public List<UsrUserVO> findUsersPermissionsByFund(@PathVariable(value = "fundId") final Integer fundId) {
        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        List<UsrUser> users = userService.findUsersByFund(fund);
        return factoryVO.createUserList(users, true);
    }

    /**
     * Načte seznam uživatelů, kteří mají explicitně (přímo na nich) nastavené nějaké oprávnění typu všechny AS.
     * @return seznam
     */
    @RequestMapping(value = "/fund/all/users", method = RequestMethod.GET)
	@Transactional
    public List<UsrUserVO> findUsersPermissionsByFundAll() {
        List<UsrUser> users = userService.findUsersByFundAll();
        return factoryVO.createUserList(users, true);
    }

    /**
     * Přidání uživatelů do skupin.
     *
     * @param params identifikátory přidávaných uživatelů a skupin
     */
    @Transactional
    @RequestMapping(value = "/group/join", method = RequestMethod.POST)
    public void joinGroup(@RequestBody final IdsParam params) {
        Set<UsrGroup> groups = userService.getGroups(params.getGroupIds());
        Set<UsrUser> users = userService.getUsers(params.getUserIds());
        userService.joinGroup(groups, users);
    }

    /**
     * Přidání uživatele do skupiny.
     *
     * @param groupId identifikátor skupiny, ze které odebírám uživatel
     * @param userId  identifikátor odebíraného uživatele
     */
    @Transactional
    @RequestMapping(value = "/group/{groupId}/leave/{userId}", method = RequestMethod.POST)
    public void leaveGroup(@PathVariable(value = "groupId") final Integer groupId,
                           @PathVariable(value = "userId") final Integer userId) {
        UsrGroup group = userService.getGroup(groupId);
        UsrUser user = userService.getUser(userId);

        userService.leaveGroup(group, user);
    }

    /**
     * Přidání oprávnění uživatele.
     *
     * @param userId      identifikátor uživatele
     * @param permissions seznam oprávnění pro přidání
     */
    @Transactional
    @RequestMapping(value = "/{userId}/permission/add", method = RequestMethod.POST)
    public List<UsrPermissionVO> addUserPermission(@PathVariable(value = "userId") final Integer userId,
                                     @RequestBody final List<UsrPermissionVO> permissions) {
        UsrUser user = userService.getUser(userId);
        List<UsrPermission> usrPermissions = factoryDO.createPermissionList(permissions);
        List<UsrPermission> result = userService.addUserPermission(user, usrPermissions, true);
        return factoryVO.createPermissionList(result, UsrUser.class);
    }

    /**
     * Odebrání oprávnění uživatele.
     *
     * @param userId      identifikátor uživatele
     * @param permission seznam oprávnění pro odebr8n9
     */
    @Transactional
    @RequestMapping(value = "/{userId}/permission/delete", method = RequestMethod.POST)
    public void deleteUserPermission(@PathVariable(value = "userId") final Integer userId,
                                     @RequestBody final UsrPermissionVO permission) {
        UsrUser user = userService.getUser(userId);
        List<UsrPermission> usrPermissions = factoryDO.createPermissionList(Collections.singletonList(permission));
        userService.deleteUserPermission(user, usrPermissions);
    }

    /**
     * Odebrání oprávnění uživatele na AS.
     *
     * @param userId      identifikátor uživatele
     * @param fundId      id AS
     */
    @Transactional
    @RequestMapping(value = "/{userId}/permission/delete/fund/{fundId}", method = RequestMethod.POST)
    public void deleteUserFundPermission(@PathVariable(value = "userId") final Integer userId, @PathVariable("fundId") final Integer fundId) {
        UsrUser user = userService.getUser(userId);
        userService.deleteUserFundPermissions(user, fundId);
    }

    /**
     * Odebrání oprávnění uživatele typu AS all.
     *
     * @param userId      identifikátor uživatele
     */
    @Transactional
    @RequestMapping(value = "/{userId}/permission/delete/fund/all", method = RequestMethod.POST)
    public void deleteUserFundAllPermission(@PathVariable(value = "userId") final Integer userId) {
        UsrUser user = userService.getUser(userId);
        userService.deleteUserFundAllPermissions(user);
    }

    /**
     * Odebrání oprávnění uživatele na typ rejstříku.
     *
     * @param userId      identifikátor uživatele
     * @param scopeId     id typu rejstříku
     */
    @Transactional
    @RequestMapping(value = "/{userId}/permission/delete/scope/{scopeId}", method = RequestMethod.POST)
    public void deleteUserScopePermission(@PathVariable(value = "userId") final Integer userId, @PathVariable("scopeId") final Integer scopeId) {
        UsrUser user = userService.getUser(userId);
        userService.deleteUserScopePermissions(user, scopeId);
    }

    /**
     * Pomocná struktura pro vytvoření uživatele.
     */
    public static class CreateUser {

        /**
         * Identifikátor osoby
         */
        private Integer partyId;

        /**
         * Uživatelské jméno
         */
        private String username;

        /**
         * Heslo
         */
        private String password;

        public Integer getPartyId() {
            return partyId;
        }

        public void setPartyId(final Integer partyId) {
            this.partyId = partyId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }
    }

    /**
     * Pomocná struktura pro změnu hesla
     */
    public static class ChangePassword {

        /**
         * Původní heslo
         */
        private String oldPassword;

        /**
         * Nové heslo
         */
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(final String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(final String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * Pomocná třída na předání identifikátorů skupin a uživatelů.
     */
    public static class IdsParam {

        /**
         * Identifikátory skupin
         */
        private Set<Integer> groupIds;

        /**
         * Identifikátory uživatelů
         */
        private Set<Integer> userIds;

        public IdsParam() {
        }

        public IdsParam(final Set<Integer> groupIds, final Set<Integer> userIds) {
            this.groupIds = groupIds;
            this.userIds = userIds;
        }

        public Set<Integer> getGroupIds() {
            return groupIds;
        }

        public void setGroupIds(final Set<Integer> groupIds) {
            this.groupIds = groupIds;
        }

        public Set<Integer> getUserIds() {
            return userIds;
        }

        public void setUserIds(final Set<Integer> userIds) {
            this.userIds = userIds;
        }
    }
}
