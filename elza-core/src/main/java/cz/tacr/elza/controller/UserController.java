package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.UserDetailVO;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

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
    public UserDetailVO getPermissions() {
        final UserDetail userDetail = userService.getLoggedUserDetail();
        return factoryVO.createUserDetail(userDetail);
    }
}
