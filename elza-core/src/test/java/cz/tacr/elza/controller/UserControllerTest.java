package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.UserInfoVO;
import org.junit.Test;
import org.springframework.util.Assert;


/**
 * @author Martin Šlapa
 * @since 11.05.2016
 */
public class UserControllerTest extends AbstractControllerTest {

    @Test
    public void userDetail() {
        UserInfoVO userDetail = getUserDetail();
        Assert.notNull(userDetail);
        Assert.notNull(userDetail.getUsername());
        Assert.notEmpty(userDetail.getUserPermissions());
    }

}
