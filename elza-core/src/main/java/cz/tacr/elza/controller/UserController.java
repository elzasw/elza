package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;

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
     * Načtení uživatele s daty pro zobrazení na detailu s možností editace.
     * @param userId id
     * @return VO
     */
    @RequestMapping(value = "/getUser", method = RequestMethod.GET)
    UsrUserVO getUser(@RequestParam(value = "userId") final Integer userId) {
        Assert.notNull(userId);

        UsrUser user = userService.getUser(userId);
        return factoryVO.createUser(user);
    }

    /**
     * Načtení skupiny s daty pro zobrazení na detailu s možností editace.
     * @param groupId id
     * @return VO
     */
    @RequestMapping(value = "/getGroup", method = RequestMethod.GET)
    UsrGroupVO getGroup(@RequestParam(value = "groupId") final Integer groupId) {
        Assert.notNull(groupId);

        UsrGroup group = userService.getGroup(groupId);
        return factoryVO.createGroup(group, true, true);
    }

    /**
     * Načte seznam uživatelů.
     * @param search hledaný řetězec
     * @param from počáteční záznam
     * @param count počet vrácených záznamů
     * @param active mají se vracet aktivní osoby?
     * @param disabled mají se vracet zakázané osoby?
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "/findUser", method = RequestMethod.GET)
    public FilteredResultVO<UsrUserVO> findUser(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                @RequestParam("active") final Boolean active,
                                                @RequestParam("disabled") final Boolean disabled,
                                                @RequestParam("from") final Integer from,
                                                @RequestParam("count") final Integer count
    ) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        FilteredResult<UsrUser> users = userService.findUser(search, active, disabled, from, count);
        List<UsrUserVO> resultVo = factoryVO.createUserList(users.getList());
        return new FilteredResultVO<UsrUserVO>(resultVo, users.getTotalCount());
    }

    /**
     * Načte seznam skupin.
     * @param search hledaný řetězec
     * @param from počáteční záznam
     * @param count počet vrácených záznamů
     * @return seznam s celkovým počtem
     */
    @RequestMapping(value = "/findGroup", method = RequestMethod.GET)
    public FilteredResultVO<UsrGroupVO> findGroup(@Nullable @RequestParam(value = "search", required = false) final String search,
                                                  @RequestParam("from") final Integer from,
                                                  @RequestParam("count") final Integer count
    ) {
        FilteredResult<UsrGroup> groups = userService.findGroup(search, from, count);
        List<UsrGroupVO> resultVo = factoryVO.createGroupList(groups.getList(), false, false);
        return new FilteredResultVO<UsrGroupVO>(resultVo, groups.getTotalCount());
    }
}
