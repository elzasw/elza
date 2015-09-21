package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemCoordinates;
import cz.tacr.elza.domain.ArrDescItemFormattedText;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemText;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrDescItems;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;

/**
 * Testy pro {@link ArrangementManager}.
 */
public class ArrangementManagerTest extends AbstractRestTest {

    private static final Logger logger = LoggerFactory.getLogger(ArrangementManagerTest.class);

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private FindingAidRepository findingAidRepository;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;
    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;
    @Autowired
    private DescItemRepository descItemRepository;
    @Autowired
    private DescItemSpecRepository descItemSpecRepository;
    @Autowired
    private DescItemTypeRepository descItemTypeRepository;
    @Autowired
    private DataTypeRepository dataTypeRepository;
    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private RuleManager ruleManager;
    @Autowired
    private NodeRepository nodeRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    @Transactional
    public void testCreateFindingAid() throws Exception {
        createFindingAid(TEST_NAME);
    }

    @Test
    @Transactional
    public void testDeleteFindingAid() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        arrangementManager.deleteFindingAid(findingAid.getFindingAidId());
    }

    @Test
    @Transactional
    public void testGetFindingAids() throws Exception {
        createFindingAid(TEST_NAME);

        Assert.assertFalse(arrangementManager.getFindingAids().isEmpty());
    }

    @Test
    @Transactional
    public void testUpdateFindingAid() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);
        findingAid.setName(TEST_NAME);

        arrangementManager.updateFindingAid(findingAid);
    }

    // ---- REST test ----
    @Test
    public void testRestCreateFindingAid() throws Exception {
        ArrFindingAid findingAid = createFindingAidRest(TEST_NAME);

        Assert.assertNotNull(findingAid);
        Assert.assertNotNull(findingAid.getFindingAidId());
    }

    @Test
    public void testRestGetFindingAid() throws Exception {
        ArrFindingAid findingAid = createFindingAidRest(TEST_NAME);

        ArrFindingAid getFindingAid = getFindingAid(findingAid.getFindingAidId());

        Assert.assertNotNull(getFindingAid);
        Assert.assertEquals(TEST_NAME, getFindingAid.getName());
        Assert.assertEquals(findingAid.getFindingAidId(), getFindingAid.getFindingAidId());
    }

    @Test
    public void testRestGetFindingAids() throws Exception {
        createFindingAidRest(TEST_NAME);

        List<ArrFindingAid> findingAids = getFindingAids();
        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !findingAids.isEmpty());
    }

    @Test
    public void testRestDeleteFindingAid() throws Exception {
        Integer idFinfingAid = createFindingAidRest(TEST_NAME).getFindingAidId();

        long countStart = findingAidRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, idFinfingAid)
                .get(DELETE_FA_URL);
        logger.info(response.asString());
        long countEnd = findingAidRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);
    }

    @Test
    public void testRestDeleteFindingAidWithMoreVersions() throws Exception {
        ArrFindingAid findingAid = createFindingAidRest(TEST_NAME);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);

        List<ArrFindingAidVersion> versions = Arrays.asList(response.getBody().as(ArrFindingAidVersion[].class));
        ArrFindingAidVersion version = versions.iterator().next();

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).
                parameter(ARRANGEMENT_TYPE_ID_ATT, version.getArrangementType().getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, version.getRuleSet().getRuleSetId()).
                get(APPROVE_VERSION_URL);

        long countStart = findingAidRepository.count();

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, findingAid.getFindingAidId())
                .get(DELETE_FA_URL);
        logger.info(response.asString());
        long countEnd = findingAidRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);
    }

    @Test
    public void testRestUpdateFindingAid() throws Exception {
        Integer idFinfingAid = createFindingAidRest(TEST_NAME).getFindingAidId();
        ArrFindingAid arrFindingAid = findingAidRepository.findOne(idFinfingAid);

        arrFindingAid.setName(TEST_UPDATE_NAME);
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(arrFindingAid)
                .put(UPDATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFindingAid updatedFindingAid = null;

        List<ArrFindingAid> findingAids = getFindingAids();
        for (ArrFindingAid findingAid : findingAids) {
            if (findingAid.getFindingAidId().equals(idFinfingAid)) {
                updatedFindingAid = findingAid;
                break;
            }
        }
        Assert.assertNotNull(updatedFindingAid);
        Assert.assertEquals(TEST_UPDATE_NAME, updatedFindingAid.getName());
    }

    @Test
    public void testRestGetArrangementTypes () throws Exception {
        RulRuleSet ruleSet = createRuleSet();
        createArrangementType(ruleSet);

        List<RulArrangementType> arrangementTypes = getArrangementTypes(ruleSet);
        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !arrangementTypes.isEmpty());
    }

    @Test
    public void testRestGetFindingAidVersions() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        int versionCount = 10;
        for (int i = 0; i < versionCount; i++) {
            createFindingAidVersion(findingAid, false, null);
        }

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<ArrFindingAidVersion> versions = Arrays.asList(response.getBody().as(ArrFindingAidVersion[].class));
        Assert.assertTrue(versions.size() == versionCount + 1);

        ArrFindingAidVersion prevVersion = null;
        for (ArrFindingAidVersion version : versions) {
            if (prevVersion == null) {
                prevVersion = version;
                continue;
            }

            if (prevVersion.getCreateChange().getChangeDate().isAfter(version.getCreateChange().getChangeDate())) {
                Assert.fail();
            }
        }
    }

    @Test
    public void testRestApproveVersion() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId())
                , GET_FINDING_AID_VERSIONS_URL);

        List<ArrFindingAidVersion> versions = Arrays.asList(response.getBody().as(ArrFindingAidVersion[].class));
        ArrFindingAidVersion version = versions.iterator().next();


        response = put(spec -> spec.body(version).
                parameter(ARRANGEMENT_TYPE_ID_ATT, version.getArrangementType().getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, version.getRuleSet().getRuleSetId())
                , APPROVE_VERSION_URL);

        ArrFindingAidVersion newVersion = response.getBody().as(ArrFindingAidVersion.class);

        response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_FINDING_AID_VERSIONS_URL);

        versions = Arrays.asList(response.getBody().as(ArrFindingAidVersion[].class));

        Assert.assertNotNull(newVersion);
        Assert.assertTrue(newVersion.getLockChange() == null);
        Assert.assertTrue(versions.size() == 2);
    }


    @Test
    public void testRestGetVersionByFa() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = createFindingAidVersion(findingAid, true, null);
        // prvni version se vytvori pri zalozeni FA
        Integer createVersionId = version.getFindingAidVersionId() - 1;
        ArrFindingAidVersion versionChange = createFindingAidVersion(findingAid, true, null);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(VERSION_ID_ATT, version.getFindingAidVersionId()).get(GET_VERSION_ID_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ArrFindingAidVersion resultVersion = response.getBody().as(ArrFindingAidVersion.class);
        Assert.assertNotNull("Version nebylo nalezeno", resultVersion);
        Assert.assertEquals(resultVersion.getFindingAidVersionId(), version.getFindingAidVersionId());

        resultVersion = getFindingAidOpenVersion(findingAid);
        Assert.assertNotNull("Version nebylo nalezeno", resultVersion);
        Assert.assertEquals(resultVersion.getFindingAidVersionId(), createVersionId);
        Assert.assertNull(resultVersion.getLockChange());
    }

    @Test
    public void testRestGetLevelByParent() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChange = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChange);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChange);

        ArrLevel child = createLevel(2, parent, version.getCreateChange());
        ArrLevel child2 = createLevel(2, parent, version.getCreateChange());
        ArrChange change = createFaChange(LocalDateTime.now());
        child2.setDeleteChange(change);
        levelRepository.save(child2);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, parent.getNode().getNodeId()).
                parameter(VERSION_ID_ATT, version.getFindingAidVersionId()).get(FIND_SUB_LEVELS_EXT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        List<ArrLevelExt> levelList = Arrays.asList(response.getBody().as(ArrLevelExt[].class));
        if (levelList.size() != 1) {
            Assert.fail();
        }

        ArrChange lockChange = createFaChange(LocalDateTime.now());
        version.setLockChange(lockChange);
        findingAidVersionRepository.save(version);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, parent.getNode().getNodeId()).
                parameter(VERSION_ID_ATT, version.getFindingAidVersionId()).get(FIND_SUB_LEVELS_EXT_URL);
        logger.info(response.asString());
        levelList = Arrays.asList(response.getBody().as(ArrLevelExt[].class));
        if (levelList.size() != 1) {
            Assert.fail();
        }
    }

    @Test
    public void testRestAddLevelBefore() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());
//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        ArrLevelWithExtraNode second = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(second.getLevel());
        levelWithExtraNode.setExtraNode(second.getExtraNode());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_BEFORE_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode first = response.getBody().as(ArrLevelWithExtraNode.class);

        List<ArrLevelExt> subLevels = arrangementManager
                .findSubLevels(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 2);

        Iterator<ArrLevelExt> iterator = subLevels.iterator();
        Assert.assertTrue(first.getLevel().getNode().getNodeId().equals(iterator.next().getNode().getNodeId()));
        Assert.assertTrue(second.getLevel().getNode().getNodeId().equals(iterator.next().getNode().getNodeId()));
    }

    @Test
    public void testRestAddLevelAfter() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());
//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode first = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(first.getLevel());
        levelWithExtraNode.setExtraNode(first.getExtraNode());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_AFTER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode second = response.getBody().as(ArrLevelWithExtraNode.class);

        List<ArrLevelExt> subLevels = arrangementManager.findSubLevels(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 2);

        Iterator<ArrLevelExt> iterator = subLevels.iterator();
        Assert.assertTrue(first.getLevel().getLevelId().equals(iterator.next().getLevelId()));
        Assert.assertTrue(second.getLevel().getLevelId().equals(iterator.next().getLevelId()));
    }

    @Test
    public void testRestAddLevelChild() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());
//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());
        levelWithExtraNode.setLevel(version.getRootLevel());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode parent = response.getBody().as(ArrLevelWithExtraNode.class);

        Integer parentNodeId = parent.getLevel().getNode().getNodeId();
        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(parent.getLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode child = response.getBody().as(ArrLevelWithExtraNode.class);

        List<ArrLevelExt> subLevels = arrangementManager.findSubLevels(parentNodeId, version.getFindingAidVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 1);

        Assert.assertTrue(child.getLevel().getLevelId().equals(subLevels.iterator().next().getLevelId()));
        Assert.assertTrue(child.getLevel().getNodeParent().getNodeId().equals(parentNodeId));
    }

    private ArrFindingAidVersion getRootNodeIdForVersion(Integer findingAidId) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAidId).get(GET_OPEN_VERSION_BY_FA_ID_URL);
        logger.info(response.asString());

        Assert.assertEquals(200, response.statusCode());
        ArrFindingAidVersion faVersion = response.getBody().as(ArrFindingAidVersion.class);
        return faVersion;
//        ArrLevel level = faVersion.getRootFaLevel();
//        ArrNode node = level.getNode();
//        return node.getNodeId();
    }

    @Test
    public void testRestMoveLevelBefore() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());

//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(response.print(), 200, response.statusCode());

        ArrLevelWithExtraNode parent = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(parent.getLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode child = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(child.getLevel());
        child.getLevel().getNode().setVersion(child.getLevel().getNode().getVersion() + 1);
        child.getLevel().getNodeParent().setVersion(child.getLevel().getNodeParent().getVersion() + 1);

        ArrNode parentNode = parent.getLevel().getNode();
        parentNode.setVersion(parentNode.getVersion() + 1);
        parent.getLevel().getNodeParent().setVersion(parent.getLevel().getNodeParent().getVersion() + 1);
        levelWithExtraNode.setLevelTarget(parent.getLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(MOVE_LEVEL_BEFORE_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode movedChild = response.getBody().as(ArrLevelWithExtraNode.class);

        List<ArrLevelExt> subLevels = arrangementManager.findSubLevels(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId(), null, null);

        Assert.assertTrue(subLevels.size() == 2);
        Assert.assertTrue(movedChild.getLevel().getNodeParent().getNodeId().equals(parent.getLevel().getNodeParent().getNodeId()));
    }

    @Test
    public void testRestMoveLevelUnder() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());
//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode first = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        version.getRootLevel().getNode().setVersion(version.getRootLevel().getNode().getVersion() + 1);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode second = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode.setLevel(first.getLevel());
        levelWithExtraNode.setExtraNode(second.getLevel().getNode());
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(MOVE_LEVEL_UNDER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode child = response.getBody().as(ArrLevelWithExtraNode.class);
        Assert.assertTrue(child.getLevel().getNodeParent().getNodeId().equals(second.getLevel().getNode().getNodeId()));

        List<ArrLevelExt> subLevels = arrangementManager.findSubLevels(second.getLevel().getNode().getNodeId(), version.getFindingAidVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 1);
        Assert.assertTrue(child.getLevel().getLevelId().equals(subLevels.iterator().next().getLevelId()));
    }

    @Test
    public void testRestMoveLevelAfter() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());
//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode parent = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(parent.getLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode child = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(child.getLevel());
        levelWithExtraNode.setLevelTarget(parent.getLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        child.getLevel().getNodeParent().setVersion(child.getLevel().getNodeParent().getVersion() + 1);
        parent.getLevel().getNode().setVersion(parent.getLevel().getNode().getVersion() + 1);
        parent.getLevel().getNodeParent().setVersion(parent.getLevel().getNodeParent().getVersion() + 1);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(MOVE_LEVEL_AFTER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode movedChild = response.getBody().as(ArrLevelWithExtraNode.class);

        List<ArrLevelExt> subLevels = arrangementManager.findSubLevels(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId(), null, null);

        Assert.assertTrue(subLevels.size() == 2);
        Assert.assertTrue(movedChild.getLevel().getNodeParent().getNodeId().equals(parent.getLevel().getNodeParent().getNodeId()));
    }

    @Test
    public void testRestDeleteLevel() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFindingAidVersion version = getRootNodeIdForVersion(findingAid.getFindingAidId());
//        ArrFindingAidVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode node = response.getBody().as(ArrLevelWithExtraNode.class);

        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(node.getLevel());
        levelWithExtraNode.setExtraNode(node.getExtraNode());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(levelWithExtraNode).put(DELETE_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrLevelWithExtraNode deletedNode = response.getBody().as(ArrLevelWithExtraNode.class);

        Assert.assertTrue(deletedNode.getLevel().getDeleteChange() != null);
        Assert.assertTrue(node.getLevel().getNode().getNodeId().equals(deletedNode.getLevel().getNode().getNodeId()));
        Assert.assertTrue(node.getLevel().getLevelId().equals(deletedNode.getLevel().getLevelId()));
    }

    private static class TestLevelData {
        private Integer descItemTypeId1;
        private Integer descItemTypeId2;
        private Integer childNodeId1;
        private Integer childNodeId2;
        private Integer versionId;

        public TestLevelData(Integer descItemTypeId1, Integer descItemTypeId2, Integer childNodeId1,
                Integer childNodeId2, Integer versionId) {
            this.descItemTypeId1 = descItemTypeId1;
            this.descItemTypeId2 = descItemTypeId2;
            this.childNodeId1 = childNodeId1;
            this.childNodeId2 = childNodeId2;
            this.versionId = versionId;
        }
        public Integer getDescItemTypeId1() {
            return descItemTypeId1;
        }
        public Integer getDescItemTypeId2() {
            return descItemTypeId2;
        }
        public Integer getChildNodeId1() {
            return childNodeId1;
        }
        public Integer getChildNodeId2() {
            return childNodeId2;
        }
        public Integer getVersionId() {
            return versionId;
        }
    }

    @Transactional
    private TestLevelData createTestLevelData() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChangeVersion = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChangeVersion);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChangeVersion);

        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrLevel child = createLevel(2, parent, createChange);
        createAttributs(child.getNode(), 1, createChange, 1, DATA_TYPE_RECORD_REF);
        levelRepository.save(child);

        version.setLockChange(createFaChange(startTime.plusSeconds(2)));
        findingAidVersionRepository.save(version);
        child.setDeleteChange(createFaChange(startTime.plusSeconds(3)));
        createChange = createFaChange(startTime.plusSeconds(3));
        createAttributs(child.getNode(), 2, createChange, 11, DATA_TYPE_STRING);

        createChange = createFaChange(startTime.plusSeconds(3));
        ArrLevel child2 = createLevel(2, parent, createChange);
        ArrDescItem item = createAttributs(child2.getNode(), 1, createChange, 2, DATA_TYPE_STRING);
        ArrDescItem item2 = createAttributs(child2.getNode(), 2, createChange, 21, DATA_TYPE_STRING);
        item2.setDeleteChange(createChange);
        descItemRepository.save(item2);

        TestLevelData result = new TestLevelData(item.getDescItemType().getDescItemTypeId(),
            item2.getDescItemType().getDescItemTypeId(), child.getNode().getNodeId(), child2.getNode().getNodeId(), version.getFindingAidVersionId());
        return result;
    }

    @Test
    public void testRestGetLevelByNodeId() {
        TestLevelData testLevel = createTestLevelData();

        Integer[] descItemTypeIds = {1, testLevel.getDescItemTypeId1(), testLevel.getDescItemTypeId2()};
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, testLevel.getChildNodeId2()).parameter("descItemTypeIds", descItemTypeIds).get(GET_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ArrLevelExt level = response.getBody().as(ArrLevelExt.class);

        if (level == null) {
            Assert.fail();
        }
        if (level.getDescItemList().size() != 1) {
            Assert.fail();
        } else {
            ArrDescItem descItem = level.getDescItemList().get(0);
//            Assert.assertNotNull(descItem.getData()); // zahadne se hodnota neserializuje, ale vrati se
        }

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, testLevel.getChildNodeId1()).
                parameter(VERSION_ID_ATT, testLevel.getVersionId()).get(GET_LEVEL_URL);
        logger.info(response.asString());
        level = response.getBody().as(ArrLevelExt.class);
        if (level == null) {
            Assert.fail();
        }

        if (level.getDescItemList().size() != 1) {
            Assert.fail();
        } else {
            ArrDescItem descItem = level.getDescItemList().get(0);
            Assert.assertNotNull(descItem.toString());
            Assert.assertNotNull(((ArrDescItemRecordRef) descItem).getRecord().getRecord());
        }
    }

    /**
     * Vytvoří položku archivní pomůcky přes REST volání.
     *
     * @return vytvořená položka
     */
    private ArrFindingAid createFindingAidRest(final String name) {
        RulRuleSet ruleSet = createRuleSet();
        RulArrangementType arrangementType = createArrangementType(ruleSet);

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_NAME_ATT, name).
                parameter("arrangementTypeId", arrangementType.getArrangementTypeId()).parameter("ruleSetId", ruleSet.getRuleSetId()).
                put(CREATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFindingAid findingAid = response.getBody().as(ArrFindingAid.class);


        return findingAid;
    }

    @Test
    public void testRestCreateDescriptionItemsForAllDataTypes() {
        RulRuleSet ruleSet = createRuleSet();
        RulArrangementType arrangementType = createArrangementType(ruleSet);
        ArrFindingAid findingAid = createFindingAid(ruleSet, arrangementType, HttpStatus.OK);
        ArrFindingAidVersion openVersion = getFindingAidOpenVersion(findingAid);

        ArrDescItemSavePack savePack = new ArrDescItemSavePack();
        savePack.setCreateNewVersion(true);
        savePack.setDeleteDescItems(new ArrayList<>());
        savePack.setFaVersionId(openVersion.getFindingAidVersionId());

        ArrNode node = openVersion.getRootLevel().getNode();
        savePack.setNode(node);

        List<ArrDescItem> descItems = new ArrayList<>();
        savePack.setDescItems(descItems);

        RegRecord record = restCreateRecord();
        ParParty party = restCreateParty();
        Map<String, RulDescItemType> itemTypes = new HashMap<>();
        List<RulDataType> dataTypes = dataTypeRepository.findAll();
        int order = 1;
        for (RulDataType dataType : dataTypes) {
            RulDescItemType descItemType = createRulDescItemType(dataType, order++);
            descItemType.setDataType(dataType);
            itemTypes.put(dataType.getCode(), descItemType);
        }

        int valuesCount = 0;
        for (String dtCode : itemTypes.keySet()) {
            RulDescItemType rulDescItemType = itemTypes.get(dtCode);
            RulDescItemTypeExt rulDescItemTypeExt = new RulDescItemTypeExt();
            BeanUtils.copyProperties(rulDescItemType, rulDescItemTypeExt);
            valuesCount++;
            switch (dtCode) {
                case DT_COORDINATES:
                    descItems.add(createCoordinatesValue(node, rulDescItemTypeExt));
                    break;
                case DT_FORMATTED_TEXT:
                    descItems.add(createFormattedTextValue(node, rulDescItemTypeExt));
                    descItems.add(createFormattedTextValue(node, rulDescItemTypeExt));
                    valuesCount++;
                    break;
                case DT_INT:
                    descItems.add(createIntValue(node, rulDescItemTypeExt));
                    break;
                case DT_PARTY_REF:
                    descItems.add(createPartyRefValue(node, rulDescItemTypeExt, party));
                    break;
                case DT_STRING:
                    descItems.add(createStringValue(node, rulDescItemTypeExt));
                    break;
                case DT_TEXT:
                    descItems.add(createTextValue(node, rulDescItemTypeExt));
                    break;
                case DT_UNITDATE:
                    descItems.add(createUnitdateValue(node, rulDescItemTypeExt));
                    break;
                case DT_UNITID:
                    descItems.add(createIntValue(node, rulDescItemTypeExt));
                    break;
                case DT_RECORD_REF:
                    descItems.add(createRecordRefValue(node, rulDescItemTypeExt, record));
                    break;
                default:
                    throw new IllegalStateException("Není definován case pro datový typ " + dtCode + " doplňte jej.");
            }
        }

        List<ArrDescItem> arrDescItemsExt = storeSavePack(savePack);
        // zkontrolovat uložení
        Assert.assertTrue(arrDescItemsExt.size() == valuesCount);
        for (ArrDescItem descItemExt : arrDescItemsExt) {
            Assert.assertNotNull(descItemExt.getDescItemId());
            Assert.assertNotNull(descItemExt.getCreateChange());
            Assert.assertNull(descItemExt.getDeleteChange());
        }

        // načtení hodnot
        ArrLevelExt rootLevel = getLevelByNodeId(node.getNodeId());
        List<ArrDescItem> descItemList = rootLevel.getDescItemList();
        Assert.assertTrue(arrDescItemsExt.size() == descItemList.size());
        for (ArrDescItem descItem : descItemList) {
            Assert.assertNotNull(descItem.getDescItemId());
            Assert.assertNotNull(descItem.getCreateChange());
            Assert.assertNull(descItem.getDeleteChange());
        }

        // update hodnot
        savePack.setNode(rootLevel.getNode());
        savePack.setDescItems(arrDescItemsExt);
        savePack.setCreateNewVersion(false);
        for (ArrDescItem descItemExt : arrDescItemsExt) {
            if (descItemExt.getDescItemType().getDataType().getCode().equals(DT_PARTY_REF)) {
                ((ArrDescItemPartyRef) descItemExt).setParty(party);
                continue;
            }
            if (descItemExt.getDescItemType().getDataType().getCode().equals(DT_RECORD_REF)) {
                ((ArrDescItemRecordRef) descItemExt).setRecord(record);
                continue;
            }
        }
        arrDescItemsExt = storeSavePack(savePack);

        // více hodnot u jednoho atributu -> změna pořadí
        rootLevel = getLevelByNodeId(node.getNodeId());
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(rootLevel);
        levelWithExtraNode.setFaVersionId(openVersion.getFindingAidVersionId());
        ArrLevelWithExtraNode childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrNode childNode = childLevelWithExtraNode.getLevel().getNode();

        savePack = new ArrDescItemSavePack();
        savePack.setCreateNewVersion(true);
        savePack.setDeleteDescItems(new ArrayList<>());
        savePack.setFaVersionId(openVersion.getFindingAidVersionId());
        savePack.setNode(childNode);

        descItems = new ArrayList<>();
        savePack.setDescItems(descItems);

        RulDescItemType descItemType = itemTypes.get(DT_FORMATTED_TEXT);

        RulDescItemTypeExt rulDescItemTypeExt = new RulDescItemTypeExt();
        BeanUtils.copyProperties(descItemType, rulDescItemTypeExt);
        descItems.add(createFormattedTextValue(childNode, rulDescItemTypeExt));
        descItems.add(createFormattedTextValue(childNode, rulDescItemTypeExt));

        arrDescItemsExt = storeSavePack(savePack);

        ArrLevelExt childLevel = getLevelByNodeId(childNode.getNodeId());
        ArrDescItem value1 = arrDescItemsExt.get(0);
        ArrDescItem value2 = arrDescItemsExt.get(1);
        Integer p1 = value1.getPosition();
        Integer p2 = value2.getPosition();
        value1.setPosition(p2);
        value2.setPosition(p1);

        savePack.setNode(childLevel.getNode());
        savePack.setDescItems(arrDescItemsExt);
    }

    private ArrDescItem createRecordRefValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt, RegRecord record) {
        ArrDescItem descItem = new ArrDescItemRecordRef();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemRecordRef) descItem).setRecord(record);
        return descItem;
    }

    private ArrDescItem createPartyRefValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt, ParParty party) {
        ArrDescItem descItem = new ArrDescItemPartyRef();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemPartyRef) descItem).setParty(party);
        return descItem;
    }

    private ArrDescItem createCoordinatesValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItem descItem = new ArrDescItemCoordinates();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemCoordinates) descItem).setValue("coordinates");
        return descItem;
    }

    private ArrDescItem createUnitdateValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItem descItem = new ArrDescItemUnitdate();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemUnitdate) descItem).setValue(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return descItem;
    }

    private ArrDescItem createIntValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItem descItem = new ArrDescItemInt();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemInt) descItem).setValue(RandomUtils.nextInt(Integer.MAX_VALUE));
        return descItem;
    }

    private ArrDescItem createValue(ArrDescItem descItem, ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        descItem = createValueWithoutspecification(descItem, node, rulDescItemTypeExt);
        descItem.setDescItemSpec(chooseSpec(rulDescItemTypeExt.getRulDescItemSpecList()));
        return descItem;
    }

    private ArrDescItem createValueWithoutspecification(ArrDescItem descItem, ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        descItem.setNode(node);
        descItem.setDescItemType(rulDescItemTypeExt);
        return descItem;
    }

    private ArrDescItem createStringValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItem descItem = new ArrDescItemString();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemString) descItem).setValue(Integer.toString(RandomUtils.nextInt(Integer.MAX_VALUE)));
        return descItem;
    }

    private ArrDescItem createTextValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItem descItem = new ArrDescItemText();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemText) descItem).setValue("Text");
        return descItem;
    }

    private ArrDescItem createFormattedTextValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItem descItem = new ArrDescItemFormattedText();
        descItem = createValue(descItem, node, rulDescItemTypeExt);
        ((ArrDescItemFormattedText) descItem).setValue("Formatted text");
        return descItem;
    }

    private RulDescItemSpec chooseSpec(List<RulDescItemSpecExt> rulDescItemSpecList) {
        if (rulDescItemSpecList == null || rulDescItemSpecList.isEmpty()) {
            return null;
        }

        int size = rulDescItemSpecList.size();
        return rulDescItemSpecList.get(RandomUtils.nextInt(size));
    }


    private RulDescItemType createRulDescItemType(RulDataType dataType, int order) {
        String code = dataType.getCode();
        String description = dataType.getDescription();
        String name = dataType.getName();
        RulDescItemType descItemType = createDescItemType(dataType, code, name, code, description, false, false,
                false, order);

        descItemType = descItemTypeRepository.save(descItemType);

        createDescItemConstrain(descItemType, null);

        return descItemType;
    }

    @Test
    public void testRestCreateDescriptionItem() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChangeVersion = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChangeVersion);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChangeVersion);

        version.setRootLevel(parent);
        findingAidVersionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, false, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu

        ArrDescItemInt descItem = new ArrDescItemInt();
        descItem.setDescItemType(descItemType);
        descItem.setDescItemSpec(descItemSpec);
        descItem.setValue(123);
        descItem.setNode(node);

        ArrDescItem descItemRet = arrangementManager.createDescriptionItem(descItem, version.getFindingAidVersionId());

        // kontrola attributu a hodnoty

        Assert.assertNotNull("Hodnotu attributu se nepodařilo vytvořit", descItemRet);

        List<ArrData> dataList = dataRepository.findByDescItem(descItemRet);

        if (dataList.size() != 1) {
            Assert.fail("Nesprávný počet položek");
        }

        ArrData data = dataList.get(0);

        if (!(data instanceof ArrDataInteger)) {
            Assert.fail("Nesprávný datový typ hodnoty");
        }

        ArrDataInteger dataInteger = (ArrDataInteger) data;

        if (!dataInteger.getValue().equals(123)) {
            Assert.fail("Vložená hodnota není identická");
        }

        // vytvoření závislých dat

        RulDescItemType descItemType2 = createDescItemType(dataType, "ITEM_TYPE2", "Item type 2", "SH3", "Desc 3", false, false, true, 2);
        RulDescItemSpec descItemSpec2 = createDescItemSpec(descItemType2, "ITEM_SPEC2", "Item spec 2", "SH4", "Desc 4", 2);
        createDescItemConstrain(descItemType2, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType2, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu - kontrola position

        node = nodeRepository.findOne(node.getNodeId());
        descItem = new ArrDescItemInt();
        descItem.setDescItemType(descItemType2);
        descItem.setDescItemSpec(descItemSpec2);
        descItem.setValue(123);
        descItem.setNode(node);

        ArrDescItem descItemRet1 = arrangementManager.createDescriptionItem(descItem, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        descItem = new ArrDescItemInt();
        descItem.setDescItemType(descItemType2);
        descItem.setDescItemSpec(descItemSpec2);
        descItem.setValue(1234);
        descItem.setPosition(1);
        descItem.setNode(node);

        ArrDescItem descItemRet2 = arrangementManager.createDescriptionItem(descItem, version.getFindingAidVersionId());

        Assert.assertNotNull(descItemRepository.findOne(descItemRet1.getDescItemId()).getDeleteChange());
        Assert.assertEquals(new Integer(1), descItemRepository.findOne(descItemRet2.getDescItemId()).getPosition());

        node = nodeRepository.findOne(node.getNodeId());
        descItem = new ArrDescItemInt();
        descItem.setDescItemType(descItemType2);
        descItem.setDescItemSpec(descItemSpec2);
        descItem.setValue(12345);
        descItem.setPosition(10);
        descItem.setNode(node);

        ArrDescItem descItemRet3 = arrangementManager.createDescriptionItem(descItem, version.getFindingAidVersionId());

        Assert.assertEquals(new Integer(3), descItemRepository.findOne(descItemRet3.getDescItemId()).getPosition());

    }

    @Test
    public void testRestUpdateDescriptionItem() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChangeVersion = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChangeVersion);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChangeVersion);

        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, false, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu

        ArrDescItemInt descItem = new ArrDescItemInt();
        descItem.setDescItemType(descItemType);
        descItem.setDescItemSpec(descItemSpec);
        descItem.setValue(123);
        descItem.setNode(node);

        ArrDescItem descItemNew = arrangementManager.createDescriptionItem(descItem, version.getFindingAidVersionId());

        // upravení hodnoty bez vytvoření verze

        ArrDescItemInt arrDescItemExt = new ArrDescItemInt();
        BeanUtils.copyProperties(descItemNew, arrDescItemExt);
        arrDescItemExt.setValue(124);
        ArrDescItem descItemRet = arrangementManager.updateDescriptionItem(arrDescItemExt ,version.getFindingAidVersionId(), false);

        // kontrola nové hodnoty attributu

        Assert.assertNotNull("Hodnotu attributu se nepodařilo vytvořit", descItemRet);

        List<ArrData> dataList = dataRepository.findByDescItem(descItemRet);

        if (dataList.size() != 1) {
            Assert.fail("Nesprávný počet položek");
        }

        ArrData data = dataList.get(0);

        if (!(data instanceof ArrDataInteger)) {
            Assert.fail("Nesprávný datový typ hodnoty");
        }

        ArrDataInteger dataInteger = (ArrDataInteger) data;

        if (!dataInteger.getValue().equals(124)) {
            Assert.fail("Vložená hodnota není identická");
        }

        // upravení hodnoty s vytvořením verze

        arrDescItemExt = new ArrDescItemInt();
        BeanUtils.copyProperties(descItemNew, arrDescItemExt);
        arrDescItemExt.setValue(125);
        descItemRet = arrangementManager.updateDescriptionItem(arrDescItemExt, version.getFindingAidVersionId(), true);

        // kontrola nové hodnoty attributu

        Assert.assertNotNull("Hodnotu attributu se nepodařilo vytvořit", descItemRet);

        dataList = dataRepository.findByDescItem(descItemRet);

        if (dataList.size() != 1) {
            Assert.fail("Nesprávný počet položek");
        }

        ArrData dataNew = dataList.get(0);

        if (!(dataNew instanceof ArrDataInteger)) {
            Assert.fail("Nesprávný datový typ hodnoty");
        }

        dataInteger = (ArrDataInteger) dataNew;

        if (!dataInteger.getValue().equals(125)) {
            Assert.fail("Vložená hodnota není identická");
        }

        Assert.assertNotEquals(data, dataNew);

    }

    @Test
    public void testRestUpdateDescriptionItemPositions() {

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChangeVersion = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChangeVersion);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChangeVersion);

        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 10);

        // přidání hodnot attributů k uzlu

        ArrDescItem descItem1 = new ArrDescItemInt();
        descItem1.setDescItemType(descItemType);
        descItem1.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem1).setValue(1);
        descItem1.setNode(node);

        descItem1 = arrangementManager.createDescriptionItem(descItem1, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItem descItem2 = new ArrDescItemInt();
        descItem2.setDescItemType(descItemType);
        descItem2.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem2).setValue(2);
        descItem2.setNode(node);

        descItem2 = arrangementManager.createDescriptionItem(descItem2, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItem descItem3 = new ArrDescItemInt();
        descItem3.setDescItemType(descItemType);
        descItem3.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem3).setValue(3);
        descItem3.setNode(node);

        descItem3 = arrangementManager.createDescriptionItem(descItem3, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItem descItem4 = new ArrDescItemInt();
        descItem4.setDescItemType(descItemType);
        descItem4.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem4).setValue(4);
        descItem4.setNode(node);

        descItem4 = arrangementManager.createDescriptionItem(descItem4, version.getFindingAidVersionId());

        // úprava pozicí

        descItem3.setPosition(1);
        ArrDescItem descItem3New = arrangementManager.updateDescriptionItem(descItem3, version.getFindingAidVersionId(), true);

        // kontrola pozice attributu
        checkChangePositionDescItem(descItem1, 2, true, null);
        checkChangePositionDescItem(descItem2, 3, true, null);
        checkChangePositionDescItem(descItem3, 1, true, null);
        checkChangePositionDescItem(descItem4, 4, false, node);

        descItem3New.setPosition(3);
        ArrDescItem descItem3New2 = arrangementManager.updateDescriptionItem(descItem3New, version.getFindingAidVersionId(), true);

        // kontrola pozice attributu
        checkChangePositionDescItem(descItem1, 1, true, null);
        checkChangePositionDescItem(descItem2, 2, true, null);
        checkChangePositionDescItem(descItem3, 3, true, null);
        checkChangePositionDescItem(descItem4, 4, false, node);

    }

    @Test
    public void testRestSaveDescriptionItems() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChangeVersion = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChangeVersion);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChangeVersion);

        version.setRootLevel(parent);
        findingAidVersionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 10);

        // přidání hodnot attributů k uzlu

        ArrDescItem descItem1 = new ArrDescItemInt();
        descItem1.setDescItemType(descItemType);
        descItem1.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem1).setValue(1);
        descItem1.setNode(node);

        ArrDescItem descItem1Save = arrangementManager.createDescriptionItem(descItem1, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItem descItem2 = new ArrDescItemInt();
        descItem2.setDescItemType(descItemType);
        descItem2.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem2).setValue(2);
        descItem2.setNode(node);

        ArrDescItem descItem2Save = arrangementManager.createDescriptionItem(descItem2, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItem descItem3 = new ArrDescItemInt();
        descItem3.setDescItemType(descItemType);
        descItem3.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem3).setValue(3);
        descItem3.setNode(node);

        ArrDescItem descItem3Save = arrangementManager.createDescriptionItem(descItem3, version.getFindingAidVersionId());

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItem descItem4 = new ArrDescItemInt();
        descItem4.setDescItemType(descItemType);
        descItem4.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem4).setValue(4);
        descItem4.setNode(node);

        ArrDescItem descItem4Save = arrangementManager.createDescriptionItem(descItem4, version.getFindingAidVersionId());

        // vytvoření změn k odeslání

        node = nodeRepository.findOne(node.getNodeId());
        ArrDescItemSavePack descItemSavePack = new ArrDescItemSavePack();

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        deleteDescItems.add(descItem1Save);

        descItem2Save.setPosition(1);
        descItems.add(descItem2Save);

        descItem3Save.setPosition(2);
        descItems.add(descItem3Save);

        ArrDescItem descItemNew1 = new ArrDescItemInt();
        descItemNew1.setDescItemType(descItemType);
        descItemNew1.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItemNew1).setValue(11);
        descItemNew1.setNode(node);
        descItemNew1.setPosition(3);
        descItems.add(descItemNew1);

        descItem4Save.setPosition(4);
        descItems.add(descItem4Save);

        ArrDescItem descItemNew2 = new ArrDescItemInt();
        descItemNew2.setDescItemType(descItemType);
        descItemNew2.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItemNew2).setValue(12);
        descItemNew2.setNode(node);
        descItemNew2.setPosition(5);
        descItems.add(descItemNew2);

        descItemSavePack.setCreateNewVersion(true);
        descItemSavePack.setFaVersionId(version.getFindingAidVersionId());
        descItemSavePack.setDescItems(descItems);
        descItemSavePack.setDeleteDescItems(deleteDescItems);
        descItemSavePack.setNode(node);

        ArrDescItems descItemsContainer = arrangementManager.saveDescriptionItems(descItemSavePack);
        List<ArrDescItem> descItemListSave = descItemsContainer.getDescItems();

        Assert.assertNotNull(descItemListSave);
        Assert.assertEquals(6, descItemListSave.size());

        for (ArrDescItem descItem : descItemListSave) {
            if (descItem.getDescItemObjectId().equals(descItem1Save.getDescItemObjectId())) {
                Assert.assertNotNull(descItem.getDeleteChange());
            } else if (descItem.getDescItemObjectId().equals(descItem2Save.getDescItemObjectId())) {
                Assert.assertEquals(descItem2Save.getPosition(), descItem.getPosition());
            } else if (descItem.getDescItemObjectId().equals(descItem3Save.getDescItemObjectId())) {
                Assert.assertEquals(descItem3Save.getPosition(), descItem.getPosition());
            } else if (descItem.getDescItemObjectId().equals(descItem4Save.getDescItemObjectId())) {
                Assert.assertEquals(descItem4Save.getPosition(), descItem.getPosition());
            } else {
                if (!descItem.getPosition().equals(3) && !descItem.getPosition().equals(5)) {
                    Assert.fail("Neplatná pozice nově přidaných položek - " + descItem.getPosition());
                }
            }
        }

    }

    /**
     * Kontroluje, zda-li se po změně pozice správně vytvořila kopie
     * @param descItem
     * @param newPosition
     */
    private void checkChangePositionDescItem(ArrDescItem descItem, int newPosition, boolean hasNewRecord, ArrNode node) {
        List<ArrDescItem> descItemList1 = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItem.getDescItemObjectId());
        if (descItemList1.size() != 1) {
            Assert.fail("Nesprávný počet položek");
        }
        ArrDescItem descItemChange = descItemList1.get(0);
        if (node != null) {
            descItemChange.setNode(node);
        }

        if (hasNewRecord) {
            Assert.assertNotEquals("Nemůže být stejný záznam, protože se provedla změna pozice", descItem.getDescItemId(), descItemChange.getDescItemId());
//            Assert.assertNotEquals("Nemůže být stejný záznam, protože se provedla změna pozice", descItem.getPosition(), descItemChange.getPosition());
        } else {
            Assert.assertEquals("Musí být stejný záznam, protože se neprovedla změna pozice", descItem.getDescItemId(), descItemChange.getDescItemId());
            Assert.assertEquals("Musí být stejný záznam, protože se neprovedla změna pozice", descItem.getPosition(), descItemChange.getPosition());
        }
        Assert.assertEquals(newPosition, descItemChange.getPosition().intValue());
    }

    @Test
    public void testRestDeleteDescriptionItem() {

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrChange createChangeVersion = createFaChange(LocalDateTime.now());
        ArrLevel parent = createLevel(1, null, createChangeVersion);
        ArrFindingAidVersion version = createFindingAidVersion(findingAid, parent, false, createChangeVersion);

        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, false, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu

        ArrDescItem descItem = new ArrDescItemInt();
        descItem.setDescItemType(descItemType);
        descItem.setDescItemSpec(descItemSpec);
        ((ArrDescItemInt) descItem).setValue(123);
        descItem.setNode(node);

        ArrDescItem descItemNew = arrangementManager.createDescriptionItem(descItem, version.getFindingAidVersionId());

        // smazání hodnoty attributu

        /*ArrDescItem descItemDel = arrangementManager.deleteDescriptionItem(descItemNew);

        Assert.assertEquals(descItemNew, descItemDel);

        Assert.assertNotNull(descItemDel.getDeleteChange());*/

    }

}
