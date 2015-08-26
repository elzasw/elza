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

import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;

/**
 * Testy pro {@link RuleManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class RulRuleSetManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GET_RS_URL = RULE_MANAGER_URL + "/getRuleSets";
    private static final String GET_DIT_URL = RULE_MANAGER_URL + "/getDescriptionItemTypes";
    private static final String GET_DIT_FOR_NODE_ID_URL =
            RULE_MANAGER_URL + "/getDescriptionItemTypesForNodeId";
    private static final String GET_FVDIT_URL = RULE_MANAGER_URL + "/getFaViewDescItemTypes";
    private static final String SAVE_FVDIT_URL = RULE_MANAGER_URL + "/saveFaViewDescItemTypes";

    @Test
    public void testRestGetRuleSets() throws Exception {
        createRuleSet();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_RS_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RulRuleSet> ruleSets = Arrays.asList(response.getBody().as(RulRuleSet[].class));

        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !ruleSets.isEmpty());
    }

    @Test
    public void testRestGetDescriptionItemTypes() throws Exception {
        createConstrain(1);
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("ruleSetId", 1).get(GET_DIT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RulDescItemTypeExt> ruleSets =
                Arrays.asList(response.getBody().as(RulDescItemTypeExt[].class));

        Assert.assertTrue("Nenalezena polozka RulDescItemTypeExt", !ruleSets.isEmpty());
        RulDescItemTypeExt itemType = ruleSets.get(0);
        Assert.assertTrue("Nenalezena polozka RulDescItemConstraint",
                !itemType.getRulDescItemConstraintList().isEmpty());
        RulDescItemSpecExt itemSpec = itemType.getRulDescItemSpecList().get(0);
        Assert.assertTrue("Nenalezena polozka RulDescItemSpecExt",
                !itemType.getRulDescItemSpecList().isEmpty());
        Assert.assertTrue("Nenalezena polozka RulDescItemSpecExt->RulDescItemConstraint",
                !itemSpec.getRulDescItemConstraintList().isEmpty());
    }

    @Test
    public void testRestGetDescriptionItemTypesForNodeId() throws Exception {
        createConstrain(2);
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("faVersionId", 2).parameter("nodeId", 1)
                .parameter("mandatory", Boolean.FALSE).get(GET_DIT_FOR_NODE_ID_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RulDescItemTypeExt> ruleSets =
                Arrays.asList(response.getBody().as(RulDescItemTypeExt[].class));

        Assert.assertTrue("Nenalezena polozka RulDescItemTypeExt", !ruleSets.isEmpty());
        RulDescItemTypeExt itemType = ruleSets.get(0);
        Assert.assertTrue("Nenalezena polozka RulDescItemConstraint",
                !itemType.getRulDescItemConstraintList().isEmpty());
        RulDescItemSpecExt itemSpec = itemType.getRulDescItemSpecList().get(0);
        Assert.assertTrue("Nenalezena polozka RulDescItemSpecExt",
                !itemType.getRulDescItemSpecList().isEmpty());
        Assert.assertTrue("Nenalezena polozka RulDescItemSpecExt->RulDescItemConstraint",
                !itemSpec.getRulDescItemConstraintList().isEmpty());
    }

    @Test
    public void testRestSaveAndGetFaViewDescItemTypes() throws Exception {
        RulRuleSet ruleSet = createRuleSet();
        ArrArrangementType arrangementType = createArrangementType();
        Integer[] descItemTypeIds = {1,2,3,4,5,6,7};
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("ruleSetId", ruleSet.getRuleSetId())
                .parameter("arrangementTypeId", arrangementType.getArrangementTypeId())
                .parameter("descItemTypeIds", descItemTypeIds)
                .get(SAVE_FVDIT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);
        ArrFaVersion version = createFindingAidVersion(findingAid, null, ruleSet, arrangementType, false);
        
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("faVersionId", version.getFaVersionId())
                .get(GET_FVDIT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<Integer> ruleSets = Arrays.asList(response.getBody().as(Integer[].class));
        Assert.assertEquals(descItemTypeIds.length, ruleSets.size());
        for (int i = 0; i < descItemTypeIds.length; i++) {
            Assert.assertEquals(descItemTypeIds[i], ruleSets.get(i));
        }
    }
}
