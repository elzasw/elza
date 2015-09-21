package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Testy pro {@link RuleManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class RulRuleSetManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testRestGetDescItemSpecById() throws Exception{
        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);


        Response response = get((spec) -> spec.parameter("descItemSpecId", descItemSpec.getDescItemSpecId()),
                GET_DESC_ITEM_SPEC);

        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());


        RulDescItemSpec responseSpec = response.getBody().as(RulDescItemSpec.class);
        Assert.assertEquals(descItemSpec, responseSpec);
    }


    @Test
    public void testRestGetRuleSets() throws Exception {
        createRuleSet();

        Response response = get(GET_RS_URL);

        List<RulRuleSet> ruleSets = Arrays.asList(response.getBody().as(RulRuleSet[].class));

        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !ruleSets.isEmpty());
    }

    @Test
    public void testRestGetDescriptionItemTypes() throws Exception {
        createConstrain(1);
        Response response = get((spec) -> spec.parameter("ruleSetId", 1), GET_DIT_URL);

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

        Response response = get((spec) -> spec.parameter("faVersionId", 2).parameter(NODE_ID_ATT , 1)
                .parameter("mandatory", Boolean.FALSE), GET_DIT_FOR_NODE_ID_URL);

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
        Integer[] descItemTypeIds = IntStream.range(0, 7).map(i -> createDescItemType(i, DATA_TYPE_INTEGER).getDescItemTypeId())
                .boxed().toArray(Integer[]::new);

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);
        Response response = get((spec) -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_OPEN_VERSION_BY_FA_ID_URL);
        ArrFindingAidVersion version = response.getBody().as(ArrFindingAidVersion.class);

        RulFaView faView = createFaView(version.getRuleSet(), version.getArrangementType(), descItemTypeIds);

        response = put((spec) -> spec.body(faView).parameter("descItemTypeIds", descItemTypeIds),
                SAVE_FVDIT_URL);

        response = get((spec) -> spec.parameter("faVersionId", version.getFindingAidVersionId()), GET_FVDIT_URL);

        FaViewDescItemTypes faViewDescItemTypes = response.getBody().as(FaViewDescItemTypes.class);
        List<RulDescItemType> ruleSets = faViewDescItemTypes.getDescItemTypes();
        Assert.assertEquals(descItemTypeIds.length, ruleSets.size());
        for (int i = 0; i < descItemTypeIds.length; i++) {
            Assert.assertEquals(descItemTypeIds[i], ruleSets.get(i).getDescItemTypeId());
        }
    }
}
