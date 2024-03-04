package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.UserService.ChangedPermissionResult;

/**
 * Kontroler pro skupiny.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 05.10.2017
 */
@RestController
@RequestMapping("/api/group")
public class GroupController {
    @Autowired
    private UserService userService;

    @Autowired
    private ClientFactoryVO factoryVO;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Načtení skupiny s daty pro zobrazení na detailu s možností editace.
     *
     * @param groupId id
     * @return VO
     */
    @RequestMapping(value = "/{groupId}", method = RequestMethod.GET)
	@Transactional
    public UsrGroupVO getGroup(@PathVariable(value = "groupId") final Integer groupId) {
        Assert.notNull(groupId, "Identifikátor skupiny musí být vyplněn");

        UsrGroup group = userService.getGroup(groupId);
        return factoryVO.createGroup(group, true, true);
    }

    /**
     * Načte seznam skupin.
     *
     * @param search hledaný řetězec
     * @param from   počáteční záznam
     * @param count  počet vrácených záznamů
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<UsrGroupVO> findGroup(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                  @RequestParam("from") final Integer from,
                                                  @RequestParam("count") final Integer count
    ) {
        FilteredResult<UsrGroup> groups = userService.findGroup(search, from, count);
        return new FilteredResultVO<>(groups.getList(),
                (entity) -> factoryVO.createGroup(entity, false, false),
                groups.getTotalCount());
    }

    /**
     * Načte seznam skupin, které mají nastavené oprávnění zakládat nový AS.
     *
     * @param search hledaný řetězec
     * @param from   počáteční záznam
     * @param count  počet vrácených záznamů
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "/withFundCreate", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<UsrGroupVO> findGroupWithFundCreate(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                  @RequestParam("from") final Integer from,
                                                  @RequestParam("count") final Integer count
    ) {
        FilteredResult<UsrGroup> groups = userService.findGroupWithFundCreate(search, from, count);
        return new FilteredResultVO<>(groups.getList(),
                (entity) -> factoryVO.createGroup(entity, false, false),
                groups.getTotalCount());
    }

    /**
     * Vytvoření skupiny.
     *
     * @param params parametry pro vytvoření skupiny
     * @return vytvořená skupina
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
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
    @RequestMapping(value = "/{groupId}", method = RequestMethod.DELETE)
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
    @RequestMapping(value = "/{groupId}", method = RequestMethod.PUT)
    @Transactional
    public UsrGroupVO changeGroup(@PathVariable(value = "groupId") final Integer groupId,
                                  @RequestBody final ChangeGroup params) {
        UsrGroup group = userService.getGroup(groupId);
        group = userService.changeGroup(group, params.getName(), params.getDescription());
        return factoryVO.createGroup(group, true, true);
    }


    /**
     * Přidání oprávnění skupiny.
     *
     * @param groupId     identifikátor skupiny
     * @param permissions seznam oprávnění pro přidání
     */
    @Transactional
    @RequestMapping(value = "/{groupId}/permission/add", method = RequestMethod.POST)
    public List<UsrPermissionVO> addGroupPermission(@PathVariable(value = "groupId") final Integer groupId,
                                              @RequestBody final List<UsrPermissionVO> permissions) {
        UsrGroup group = userService.getGroup(groupId);
        List<UsrPermission> usrPermissions = factoryDO.createPermissionList(permissions);
        ChangedPermissionResult result = userService.addGroupPermission(group, usrPermissions, true);

        StaticDataProvider staticData = staticDataService.getData();
        List<UsrPermissionVO> resultVOs = result.getNewPermissions()
                .stream().map(p -> UsrPermissionVO.newInstance(p, true, staticData))
                .collect(Collectors.toList());
        return resultVOs;
    }

    /**
     * Odebrání oprávnění skupiny.
     *
     * @param groupId     identifikátor skupiny
     * @param permission seznam oprávnění pro odebr8n9
     */
    @Transactional
    @RequestMapping(value = "/{groupId}/permission/delete", method = RequestMethod.POST)
    public void deleteGroupPermission(@PathVariable(value = "groupId") final Integer groupId,
                                      @RequestBody final UsrPermissionVO permission) {
        UsrGroup group = userService.getGroup(groupId);
        List<UsrPermission> usrPermissions = factoryDO.createPermissionList(Collections.singletonList(permission));
        userService.deleteGroupPermission(group, usrPermissions);
    }

    /**
     * Odebrání oprávnění uživatele na AS.
     *
     * @param groupId identifikátor skupiny
     * @param fundId  id AS
     */
    @Transactional
    @RequestMapping(value = "/{groupId}/permission/delete/fund/{fundId}", method = RequestMethod.POST)
    public void deleteGroupFundPermission(@PathVariable(value = "groupId") final Integer groupId, @PathVariable("fundId") final Integer fundId) {
        UsrGroup group = userService.getGroup(groupId);
        userService.deleteGroupFundPermissions(group, fundId);
    }

    /**
     * Odebrání oprávnění uživatele typu AS all.
     *
     * @param groupId identifikátor skupiny
     */
    @Transactional
    @RequestMapping(value = "/{groupId}/permission/delete/fund/all", method = RequestMethod.POST)
    public void deleteGroupFundAllPermission(@PathVariable(value = "groupId") final Integer groupId) {
        UsrGroup group = userService.getGroup(groupId);
        userService.deleteGroupFundAllPermissions(group);
    }

    /**
     * Odebrání oprávnění uživatele na typ rejstříku.
     *
     * @param groupId identifikátor skupiny
     * @param scopeId id typu rejstříku
     */
    @Transactional
    @RequestMapping(value = "/{groupId}/permission/delete/scope/{scopeId}", method = RequestMethod.POST)
    public void deleteGroupScopePermission(@PathVariable(value = "groupId") final Integer groupId, @PathVariable("scopeId") final Integer scopeId) {
        UsrGroup group = userService.getGroup(groupId);
        userService.deleteGroupScopePermissions(group, scopeId);
    }

    /**
     * Načte seznam skupin, kteří mají explicitně (přímo na nich) nastavené nějaké oprávnění pro daný AS.
     * @param fundId id AS
     * @return seznam
     */
    @RequestMapping(value = "/fund/{fundId}/groups", method = RequestMethod.GET)
    public List<UsrGroupVO> findGroupsPermissionsByFund(@PathVariable(value = "fundId") final Integer fundId) {
        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        List<UsrGroup> groups = userService.findGroupsByFund(fund);
        return factoryVO.createGroupList(groups, true, false);
    }

    /**
     * Načte seznam skupin, kteří mají explicitně (přímo na nich) nastavené nějaké oprávnění typu všechny AS.
     * @return seznam
     */
    @RequestMapping(value = "/fund/all/groups", method = RequestMethod.GET)
    public List<UsrGroupVO> findGroupssPermissionsByFundAll() {
        List<UsrGroup> groups = userService.findGroupsByFundAll();
        return factoryVO.createGroupList(groups, true, false);
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
}
