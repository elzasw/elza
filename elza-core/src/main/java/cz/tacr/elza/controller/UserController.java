package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UISettingsVO;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

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

    /**
     * Získání oprávnění uživatele.
     *
     * @return výčet oprávnění uživatele.
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
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
        Assert.notNull(params);

        UsrUser user = userService.createUser(params.getUsername(), params.getPassword(), params.getPartyId());
        return factoryVO.createUser(user);
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
        Assert.notNull(params);

        UsrUser user = userService.getUser(userId);

        if (user == null) {
            throw new IllegalArgumentException("Uživatel neexistuje");
        }

        user = userService.changeUser(user, params.getUsername(), params.getPassword());
        return factoryVO.createUser(user);
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
        Assert.notNull(params);

        UsrUser user = userService.getUser(userId);

        if (user == null) {
            throw new IllegalArgumentException("Uživatel neexistuje");
        }

        user = userService.changePassword(user, params.getNewPassword());
        return factoryVO.createUser(user);
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
        Assert.notNull(params);

        UsrUser user = userService.getLoggedUser();

        if (user == null) {
            throw new IllegalArgumentException("Uživatel není přihlášen");
        }

        if (StringUtils.isEmpty(params.getOldPassword())) {
            throw new IllegalArgumentException("Je nutné zadat původní heslo");
        }

        if (StringUtils.isEmpty(params.getNewPassword())) {
            throw new IllegalArgumentException("Je nutné zadat nové heslo");
        }

        user = userService.changePassword(user, params.getOldPassword(), params.getNewPassword());
        return factoryVO.createUser(user);
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
        return factoryVO.createUser(user);
    }

    /**
     * Načtení uživatele s daty pro zobrazení na detailu s možností editace.
     *
     * @param userId id
     * @return VO
     */
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public UsrUserVO getUser(@PathVariable(value = "userId") final Integer userId) {
        Assert.notNull(userId);

        UsrUser user = userService.getUser(userId);
        return factoryVO.createUser(user);
    }

    /**
     * Načtení skupiny s daty pro zobrazení na detailu s možností editace.
     *
     * @param groupId id
     * @return VO
     */
    @RequestMapping(value = "/group/{groupId}", method = RequestMethod.GET)
    public UsrGroupVO getGroup(@PathVariable(value = "groupId") final Integer groupId) {
        Assert.notNull(groupId);

        UsrGroup group = userService.getGroup(groupId);
        return factoryVO.createGroup(group, true, true);
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
    public FilteredResultVO<UsrUserVO> findUser(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                @RequestParam("active") final Boolean active,
                                                @RequestParam("disabled") final Boolean disabled,
                                                @RequestParam("from") final Integer from,
                                                @RequestParam("count") final Integer count,
                                                @RequestParam("excludedGroupId") final Integer excludedGroupId
    ) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        FilteredResult<UsrUser> users = userService.findUser(search, active, disabled, from, count, excludedGroupId);
        List<UsrUserVO> resultVo = factoryVO.createUserList(users.getList());
        return new FilteredResultVO<>(resultVo, users.getTotalCount());
    }

    /**
     * Načte seznam skupin.
     *
     * @param search hledaný řetězec
     * @param from   počáteční záznam
     * @param count  počet vrácených záznamů
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "/group", method = RequestMethod.GET)
    public FilteredResultVO<UsrGroupVO> findGroup(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                  @RequestParam("from") final Integer from,
                                                  @RequestParam("count") final Integer count
    ) {
        FilteredResult<UsrGroup> groups = userService.findGroup(search, from, count);
        List<UsrGroupVO> resultVo = factoryVO.createGroupList(groups.getList(), false, false);
        return new FilteredResultVO<>(resultVo, groups.getTotalCount());
    }

    /**
     * Vytvoření skupiny.
     *
     * @param params parametry pro vytvoření skupiny
     * @return vytvořená skupina
     */
    @RequestMapping(value = "/group", method = RequestMethod.POST)
    @Transactional
    public UsrGroupVO createGroup(@RequestBody CreateGroup params) {
        UsrGroup group = userService.createGroup(params.getName(), params.getCode(), params.getDescription());
        return factoryVO.createGroup(group, true, true);
    }

    /**
     * Smazání skupiny.
     *
     * @param groupId identifikátor skupiny
     */
    @RequestMapping(value = "/group/{groupId}", method = RequestMethod.DELETE)
    @Transactional
    public void deleteGroup(@PathVariable(value = "groupId") final Integer groupId) {
        UsrGroup group = userService.getGroup(groupId);

        if (group == null) {
            throw new IllegalArgumentException("Skupina neexistuje");
        }

        userService.deleteGroup(group);
    }

    /**
     * Změna skupiny.
     *
     * @param groupId identifikátor skupiny
     * @param params  parametry změny skupiny
     */
    @RequestMapping(value = "/group/{groupId}", method = RequestMethod.PUT)
    @Transactional
    public UsrGroupVO changeGroup(@PathVariable(value = "groupId") final Integer groupId,
                                  @RequestBody final ChangeGroup params) {
        UsrGroup group = userService.getGroup(groupId);
        group = userService.changeGroup(group, params.getName(), params.getDescription());
        return factoryVO.createGroup(group, true, true);
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
     * Nastavení oprávnění uživatele.
     *
     * @param userId      identifikátor uživatele
     * @param permissions seznam oprávnění
     */
    @Transactional
    @RequestMapping(value = "/{userId}/permission", method = RequestMethod.POST)
    public void changeUserPermission(@PathVariable(value = "userId") final Integer userId,
                                     @RequestBody final Permissions permissions) {
        UsrUser user = userService.getUser(userId);
        List<UsrPermission> usrPermissions = factoryDO.createPermissionList(permissions.getPermissions());
        userService.changeUserPermission(user, usrPermissions);
    }

    /**
     * Nastavení oprávnění skupiny.
     *
     * @param groupId     identifikátor skupiny
     * @param permissions seznam oprávnění
     */
    @Transactional
    @RequestMapping(value = "/group/{groupId}/permission", method = RequestMethod.POST)
    public void changeGroupPermission(@PathVariable(value = "groupId") final Integer groupId,
                                      @RequestBody final Permissions permissions) {
        UsrGroup group = userService.getGroup(groupId);
        List<UsrPermission> usrPermissions = factoryDO.createPermissionList(permissions.getPermissions());
        userService.changeGroupPermission(group, usrPermissions);
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
     * Pomocná struktura pro vytvoření skupiny.
     */
    public static class CreateGroup {

        /**
         * název skupiny
         */
        private String name;

        /**
         * kód skupiny
         */
        private String code;

        /** Popis. */
        private String description;

        public CreateGroup() {
        }

        public CreateGroup(final String name, final String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Pomocná struktura pro vytvoření skupiny.
     */
    public static class ChangeGroup {

        /**
         * název skupiny
         */
        private String name;

        /**
         * popis skupiny
         */
        private String description;

        public ChangeGroup() {
        }

        public ChangeGroup(final String name, final String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
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

    /**
     * Seznam oprávnění.
     */
    public static class Permissions {

        /**
         * Seznam oprávnění.
         */
        private List<UsrPermissionVO> permissions;

        public List<UsrPermissionVO> getPermissions() {
            return permissions;
        }

        public void setPermissions(final List<UsrPermissionVO> permissions) {
            this.permissions = permissions;
        }
    }
}
