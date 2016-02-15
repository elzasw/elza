package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Testy pro pravidla.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class RulRuleSetManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;
    @Autowired
    private ArrDescItemsPostValidator descItemsPostValidator;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private DescItemFactory descItemFactory;

    @Test
    public void testRestGetDescItemSpecById() throws Exception{
        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 60001);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 60001);


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
        createConstrain(72001);
        Response response = get((spec) -> spec.parameter("ruleSetId", 1), GET_DIT_URL);

        List<RulDescItemTypeExt> ruleSets =
                Arrays.asList(response.getBody().as(RulDescItemTypeExt[].class));

        Assert.assertTrue("Nenalezena polozka RulDescItemTypeExt", !ruleSets.isEmpty());
    }

    @Test
    public void testRestGetDescriptionItemTypesForNodeId() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = findingAidVersionRepository
                .findByFindingAidIdAndLockChangeIsNull(findingAid.getFindingAidId());

        descItemTypeRepository.findAll();
        descItemSpecRepository.findAll();

        createDescItemConstrain(descItemTypeRepository.getOneByCode("ZP2015_LEVEL_TYPE"),
                descItemSpecRepository.getOneByCode("ZP2015_LEVEL_SERIES"), "CODEXXX");

        List<ArrNode> all = nodeRepository.findAll();

        Response response = post(
                (spec) -> spec.pathParameter("faVersionId", version.getFindingAidVersionId()).pathParameter(
                        NODE_ID_ATT, all.get(0).getNodeId()).body(new HashSet<String>()),
                GET_DIT_FOR_NODE_ID_URL);

        List<RulDescItemTypeExt> ruleSets =
                Arrays.asList(response.getBody().as(RulDescItemTypeExt[].class));

        Assert.assertTrue("Nenalezena polozka RulDescItemTypeExt", !ruleSets.isEmpty());
    }

    @Test
    public void testRestSaveAndGetFaViewDescItemTypes() throws Exception {
        Integer[] descItemTypeIds = IntStream.range(76001, 76007).map(i -> createDescItemType(i, DATA_TYPE_INTEGER).getDescItemTypeId())
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

    @Test
    @Transactional
    public void testConformityInfo() throws Exception{

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, false, null);

        ArrChange faChange = createFaChange(LocalDateTime.now());
        ArrLevel level1 = createLevel(1, version.getRootLevel(), faChange);
        ArrLevel level2 = createLevel(1, level1, faChange);
        ArrLevel level21 = createLevel(2, level1, faChange);
        ArrLevel level3 = createLevel(1, level2, faChange);

        ArrNode rootNode = version.getRootLevel().getNode();
        ArrNode node1 = level1.getNode();
        ArrNode node2 = level2.getNode();
        ArrNode node21 = level21.getNode();
        ArrNode node3 = level3.getNode();


        List<ArrNode> result = nodeRepository.findNodesByDirection(level2.getNode(), version, RelatedNodeDirection.PARENT);
        Assert.assertTrue(result.size() == 1 && result.contains(node1));

        result = nodeRepository.findNodesByDirection(level2.getNode(), version, RelatedNodeDirection.ASCENDANTS);
        Assert.assertTrue(result.size() == 2 && result.contains(rootNode) && result.contains(node1));

        result = nodeRepository.findNodesByDirection(level1.getNode(), version, RelatedNodeDirection.CHILDREN);
        Assert.assertTrue(result.size() == 2 && result.contains(node2) && result.contains(node21));

        result = nodeRepository.findNodesByDirection(level1.getNode(), version, RelatedNodeDirection.DESCENDANTS);
        Assert.assertTrue(result.size() == 3 && result.contains(node2) && result.contains(node21) && result.contains(node3));

        result = nodeRepository.findNodesByDirection(level2.getNode(), version, RelatedNodeDirection.SIBLINGS);
        Assert.assertTrue(result.size() == 1 && result.contains(node21));


        ArrDescItem attributs = createAttributs(node21, 1, faChange, 75002, DATA_TYPE_STRING);
        RulDescItemType descItemType = createDescItemType(750020, getDataType(DATA_TYPE_TEXT).getDataTypeId());


        ArrNodeConformity nodeConformityInfo = createNodeConformityInfo(node21, version);
        createNodeConformityMissing(nodeConformityInfo, descItemType);
        createNodeConformityError(nodeConformityInfo, attributs);


        Assert.assertTrue(nodeConformityInfoRepository.findAll().size() > 0);

        ruleService.conformityInfo(version.getFindingAidVersionId(), Arrays.asList(node21.getNodeId()),
                NodeTypeOperation.DELETE_NODE, null, null, null);

        Assert.assertTrue(nodeConformityInfoRepository.findAll().size() == 0);
    }

    @Test
    @Transactional
    public void testSetConformityInfo() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);
        ArrChange faVersionChange = createFaChange(LocalDateTime.now());
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, false, faVersionChange);

        ArrChange faChange = createFaChange(LocalDateTime.now());
        ArrLevel level = createLevel(1, version.getRootLevel(), faChange);


        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false,
                false, true, 72001);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2",
                72001);

        createDescItemConstrain(descItemType, null, version, null, "[0-9]*", null, "CODE1");
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 5, "CODE2");

        ArrDescItem descItem = createArrDescItem(faChange, null, null, descItemType, descItemSpec, level.getNode(), 1);

        ArrDataInteger dataInteger = new ArrDataInteger();
        dataInteger.setValue(123);
        dataInteger.setDescItem(descItem);
        dataInteger.setDataType(dataTypeRepository.getOne(DATA_TYPE_INTEGER));
        dataRepository.save(dataInteger);


        List<DataValidationResult> validationResults = descItemsPostValidator.postValidateNodeDescItems(level, version,
                new HashSet<String>());
        Assert.assertTrue(validationResults.size() == 2);

        ArrDataInteger data = (ArrDataInteger) dataRepository.findByDescItem(descItem).iterator().next();
        data.setValue(123456);
        dataRepository.save(data);

        ruleService.setConformityInfo(level.getLevelId(), version.getFindingAidVersionId(), new HashSet<String>());

        Assert.assertTrue(nodeConformityInfoRepository.findAll().size() > 0);
        Assert.assertTrue(nodeConformityErrorsRepository.findAll().size() > 0);

    }

    @Test
    public void testRestDataTypes() {
        List<RulDataTypeVO> dataTypes = getDataTypes();
        Assert.assertNotNull(dataTypes);
        Assert.assertEquals(12, dataTypes.size());
    }

    @Test
    public void testRestPacketTypes() {
        List<RulPacketTypeVO> packetTypes = getPacketTypes();
        Assert.assertNotNull(packetTypes);
        Assert.assertEquals(3, packetTypes.size());
    }
}
