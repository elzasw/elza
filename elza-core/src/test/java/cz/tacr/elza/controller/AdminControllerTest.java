package cz.tacr.elza.controller;

import org.junit.Test;
import org.springframework.util.Assert;

import com.jayway.restassured.response.Response;


/**
 * Testování metod z AdminController.
 *
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class AdminControllerTest extends AbstractControllerTest {

    @Test
    public void reindexTest() {
        get(REINDEX);
    }

    @Test
    public void reindexStatusTest() {
        Response response = get(REINDEX_STATUS);
        Boolean status = response.getBody().as(Boolean.class);
        Assert.notNull(status);
    }

}
