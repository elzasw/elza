package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.UserDetailVO;
import cz.tacr.elza.controller.vo.UserVO;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public UserDetailVO getUserDetail() {
        final UserDetail userDetail = userService.getLoggedUserDetail();
        return factoryVO.createUserDetail(userDetail);
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
    public FilteredResultVO<UserVO> findUser(@Nullable @RequestParam(value = "search", required = false) final String search,
                                             @RequestParam("active") final Boolean active,
                                             @RequestParam("disabled") final Boolean disabled,
                                             @RequestParam("from") final Integer from,
                                             @RequestParam("count") final Integer count
    ) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        FilteredResult<UsrUser> users = userService.findUser(search, active, disabled, from, count);
        List<UserVO> resultVo = factoryVO.createUserList(users.getList());
        return new FilteredResultVO<UserVO>(resultVo, users.getTotalCount());
    }

}
