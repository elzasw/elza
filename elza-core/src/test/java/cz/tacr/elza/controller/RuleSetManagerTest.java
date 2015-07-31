package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.RuleSet;

/**
 * Testy pro {@link RuleSetManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class RuleSetManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GET_RS_URL = RULE_SET_MANAGER_URL + "/getRuleSets";

    @Autowired
    private RuleSetManager ruleSetManager;

    @Test
    public void testRestGetRuleSets() throws Exception {
        createRuleSet();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_RS_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RuleSet> ruleSets = Arrays.asList(response.getBody().as(RuleSet[].class));

        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !ruleSets.isEmpty());
    }
}
