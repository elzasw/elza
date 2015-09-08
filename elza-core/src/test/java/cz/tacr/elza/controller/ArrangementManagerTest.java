package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Testy pro {@link ArrangementManager}.
 */
public class ArrangementManagerTest extends AbstractRestTest {

    private static final Logger logger = LoggerFactory.getLogger(ArrangementManagerTest.class);

    public static final String CREATE_FA_URL = ARRANGEMENT_MANAGER_URL + "/createFindingAid";
    public static final String UPDATE_FA_URL = ARRANGEMENT_MANAGER_URL + "/updateFindingAid";
    public static final String DELETE_FA_URL = ARRANGEMENT_MANAGER_URL + "/deleteFindingAid";
    public static final String GET_FA_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAids";
    public static final String GET_FA_ONE_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAid";
    public static final String GET_ARRANGEMENT_TYPES_URL = RULE_MANAGER_URL + "/getArrangementTypes";
    public static final String GET_FINDING_AID_VERSIONS_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAidVersions";
    public static final String APPROVE_VERSION_URL = ARRANGEMENT_MANAGER_URL + "/approveVersion";
    public static final String GET_VERSION_ID_URL = ARRANGEMENT_MANAGER_URL + "/getVersion";
    public static final String GET_VERSION_BY_FA_ID_URL = ARRANGEMENT_MANAGER_URL + "/getOpenVersionByFindingAidId";
    public static final String FIND_SUB_LEVELS_EXT_URL = ARRANGEMENT_MANAGER_URL + "/findSubLevelsExt";

    public static final String ADD_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/addLevel";
    public static final String ADD_LEVEL_BEFORE_URL = ARRANGEMENT_MANAGER_URL + "/addLevelBefore";
    public static final String ADD_LEVEL_AFTER_URL = ARRANGEMENT_MANAGER_URL + "/addLevelAfter";
    public static final String ADD_LEVEL_CHILD_URL = ARRANGEMENT_MANAGER_URL + "/addLevelChild";
    public static final String MOVE_LEVEL_BEFORE_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelBefore";
    public static final String MOVE_LEVEL_UNDER_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelUnder";
    public static final String MOVE_LEVEL_AFTER_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelAfter";
    public static final String DELETE_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/deleteLevel";
    public static final String FIND_LEVEL_BY_NODE_ID_URL = ARRANGEMENT_MANAGER_URL + "/findLevelByNodeId";
    public static final String GET_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/getLevel";

    public static final String FA_NAME_ATT = "name";
    public static final String FA_ID_ATT = "findingAidId";
    public static final String ARRANGEMENT_TYPE_ID_ATT = "arrangementTypeId";
    public static final String RULE_SET_ID_ATT = "ruleSetId";
    public static final String NODE_ID_ATT = "nodeId";
    public static final String PARENT_NODE_ID_ATT = "parentNodeId";
    public static final String FOLLOWER_NODE_ID_ATT = "followerNodeId";
    public static final String PREDECESSOR_NODE_ID_ATT = "predecessorNodeId";
    public static final String VERSION_ID_ATT = "versionId";

    public static final Integer DATA_TYPE_INTEGER = 1;
    public static final Integer DATA_TYPE_STRING = 2;
    public static final Integer DATA_TYPE_TEXT = 3;
    public static final Integer DATA_TYPE_DATACE = 4;
    public static final Integer DATA_TYPE_REF = 5;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private FindingAidRepository findingAidRepository;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private VersionRepository versionRepository;
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
    public void testRestDeleteFindingAidWithMoreVeresions() throws Exception {
        ArrFindingAid findingAid = createFindingAidRest(TEST_NAME);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);

        List<ArrFaVersion> versions = Arrays.asList(response.getBody().as(ArrFaVersion[].class));
        ArrFaVersion version = versions.iterator().next();

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
        ArrArrangementType arrangementType = new ArrArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_ARRANGEMENT_TYPES_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<ArrArrangementType> arrangementTypes = Arrays.asList(response.getBody().as(ArrArrangementType[].class));
        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !arrangementTypes.isEmpty());
    }

    @Test
    public void testRestGetFindingAidVersions() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        int versionCount = 10;
        for (int i = 0; i < versionCount; i++) {
            createFindingAidVersion(findingAid, false);
        }

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<ArrFaVersion> versions = Arrays.asList(response.getBody().as(ArrFaVersion[].class));
        Assert.assertTrue(versions.size() == versionCount + 1);

        ArrFaVersion prevVersion = null;
        for (ArrFaVersion version : versions) {
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

        List<ArrFaVersion> versions = Arrays.asList(response.getBody().as(ArrFaVersion[].class));
        ArrFaVersion version = versions.iterator().next();


        response = put(spec -> spec.body(version).
                parameter(ARRANGEMENT_TYPE_ID_ATT, version.getArrangementType().getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, version.getRuleSet().getRuleSetId())
                , APPROVE_VERSION_URL);

        ArrFaVersion newVersion = response.getBody().as(ArrFaVersion.class);

        response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_FINDING_AID_VERSIONS_URL);

        versions = Arrays.asList(response.getBody().as(ArrFaVersion[].class));

        Assert.assertNotNull(newVersion);
        Assert.assertTrue(newVersion.getLockChange() == null);
        Assert.assertTrue(versions.size() == 2);
    }


    @Test
    public void testRestGetVersionByFa() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = createFindingAidVersion(findingAid, true);
        // prvni version se vytvori pri zalozeni FA
        Integer createVersionId = version.getFaVersionId() - 1;
        ArrFaVersion versionChange = createFindingAidVersion(findingAid, true);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(VERSION_ID_ATT, version.getFaVersionId()).get(GET_VERSION_ID_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ArrFaVersion resultVersion = response.getBody().as(ArrFaVersion.class);
        Assert.assertNotNull("Version nebylo nalezeno", resultVersion);
        Assert.assertEquals(resultVersion.getFaVersionId(), version.getFaVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_VERSION_BY_FA_ID_URL);
        logger.info(response.asString());

        Assert.assertEquals(200, response.statusCode());

        resultVersion = response.getBody().as(ArrFaVersion.class);
        Assert.assertNotNull("Version nebylo nalezeno", resultVersion);
        Assert.assertEquals(resultVersion.getFaVersionId(), createVersionId);
        Assert.assertNull(resultVersion.getLockChange());
    }

    @Test
    public void testRestGetLevelByParent() throws Exception {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
        versionRepository.save(version);

        ArrFaLevel child = createLevel(2, parent, version.getCreateChange());
        ArrFaLevel child2 = createLevel(2, parent, version.getCreateChange());
        ArrFaChange change = createFaChange(LocalDateTime.now());
        child2.setDeleteChange(change);
        levelRepository.save(child2);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, parent.getNode().getNodeId()).
                parameter(VERSION_ID_ATT, version.getFaVersionId()).get(FIND_SUB_LEVELS_EXT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        List<ArrFaLevelExt> levelList = Arrays.asList(response.getBody().as(ArrFaLevelExt[].class));
        if (levelList.size() != 1) {
            Assert.fail();
        }

        ArrFaChange lockChange = createFaChange(LocalDateTime.now());
        version.setLockChange(lockChange);
        versionRepository.save(version);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, parent.getNode().getNodeId()).
                parameter(VERSION_ID_ATT, version.getFaVersionId()).get(FIND_SUB_LEVELS_EXT_URL);
        logger.info(response.asString());
        levelList = Arrays.asList(response.getBody().as(ArrFaLevelExt[].class));
        if (levelList.size() != 1) {
            Assert.fail();
        }
    }

    @Test
    public void testRestAddLevelBefore() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel second = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, second.getNode().getNodeId()).put(ADD_LEVEL_BEFORE_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel first = response.getBody().as(ArrFaLevel.class);

        List<ArrFaLevelExt> subLevels = arrangementManager
                .findSubLevels(version.getRootFaLevel().getNode().getNodeId(), version.getFaVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 2);

        Iterator<ArrFaLevelExt> iterator = subLevels.iterator();
        Assert.assertTrue(first.getNode().getNodeId().equals(iterator.next().getNode().getNodeId()));
        Assert.assertTrue(second.getNode().getNodeId().equals(iterator.next().getNode().getNodeId()));
    }

    @Test
    public void testRestAddLevelAfter() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel first = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, first.getNode().getNodeId()).put(ADD_LEVEL_AFTER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel second = response.getBody().as(ArrFaLevel.class);

        List<ArrFaLevelExt> subLevels = arrangementManager.findSubLevels(version.getRootFaLevel().getNode().getNodeId(), version.getFaVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 2);

        Iterator<ArrFaLevelExt> iterator = subLevels.iterator();
        Assert.assertTrue(first.getFaLevelId().equals(iterator.next().getFaLevelId()));
        Assert.assertTrue(second.getFaLevelId().equals(iterator.next().getFaLevelId()));
    }

    @Test
    public void testRestAddLevelChild() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel parent = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, parent.getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel child = response.getBody().as(ArrFaLevel.class);

        List<ArrFaLevelExt> subLevels = arrangementManager.findSubLevels(parent.getNode().getNodeId(), version.getFaVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 1);

        Assert.assertTrue(child.getFaLevelId().equals(subLevels.iterator().next().getFaLevelId()));
        Assert.assertTrue(child.getParentNode().getNodeId().equals(parent.getNode().getNodeId()));
    }

    @Test
    public void testRestMoveLevelBefore() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel parent = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, parent.getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel child = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, child.getNode().getNodeId()).
                parameter(FOLLOWER_NODE_ID_ATT, parent.getNode().getNodeId()).put(MOVE_LEVEL_BEFORE_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel movedChild = response.getBody().as(ArrFaLevel.class);

        List<ArrFaLevelExt> subLevels = arrangementManager.findSubLevels(version.getRootFaLevel().getNode().getNodeId(), version.getFaVersionId(), null, null);

        Assert.assertTrue(subLevels.size() == 2);
        Assert.assertTrue(movedChild.getParentNode().getNodeId().equals(parent.getParentNode().getNodeId()));
    }

    @Test
    public void testRestMoveLevelUnder() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel first = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel second = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, first.getNode().getNodeId()).
                parameter(PARENT_NODE_ID_ATT, second.getNode().getNodeId()).put(MOVE_LEVEL_UNDER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel child = response.getBody().as(ArrFaLevel.class);
        Assert.assertTrue(child.getParentNode().getNodeId().equals(second.getNode().getNodeId()));

        List<ArrFaLevelExt> subLevels = arrangementManager.findSubLevels(second.getNode().getNodeId(), version.getFaVersionId(), null, null);
        Assert.assertTrue(subLevels.size() == 1);
        Assert.assertTrue(child.getFaLevelId().equals(subLevels.iterator().next().getFaLevelId()));
    }

    @Test
    public void testRestMoveLevelAfter() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel parent = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, parent.getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel child = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, child.getNode().getNodeId()).
                parameter(PREDECESSOR_NODE_ID_ATT, parent.getNode().getNodeId()).put(MOVE_LEVEL_AFTER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel movedChild = response.getBody().as(ArrFaLevel.class);

        List<ArrFaLevelExt> subLevels = arrangementManager.findSubLevels(version.getRootFaLevel().getNode().getNodeId(), version.getFaVersionId(), null, null);

        Assert.assertTrue(subLevels.size() == 2);
        Assert.assertTrue(movedChild.getParentNode().getNodeId().equals(parent.getParentNode().getNodeId()));
    }

    @Test
    public void testRestDeleteLevel() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(NODE_ID_ATT, version.getRootFaLevel().getNode().getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel node = response.getBody().as(ArrFaLevel.class);

        response = given().parameter(NODE_ID_ATT, node.getNode().getNodeId()).put(DELETE_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFaLevel deletedNode = response.getBody().as(ArrFaLevel.class);

        Assert.assertTrue(deletedNode.getDeleteChange() != null);
        Assert.assertTrue(node.getNode().getNodeId().equals(deletedNode.getNode().getNodeId()));
        Assert.assertTrue(node.getFaLevelId().equals(deletedNode.getFaLevelId()));
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

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
//        versionRepository.save(version);   // problem optimisticke zamky
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrFaChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrFaLevel child = createLevel(2, parent, createChange);
        createAttributs(child.getNode(), 1, createChange, 1, DATA_TYP_RECORD);
        levelRepository.save(child);

        version.setLockChange(createFaChange(startTime.plusSeconds(2)));
        versionRepository.save(version);
        child.setDeleteChange(createFaChange(startTime.plusSeconds(3)));
        createChange = createFaChange(startTime.plusSeconds(3));
        createAttributs(child.getNode(), 2, createChange, 11, null);

        createChange = createFaChange(startTime.plusSeconds(3));
        ArrFaLevel child2 = createLevel(2, parent, createChange);
        ArrDescItem item = createAttributs(child2.getNode(), 1, createChange, 2, null);
        ArrDescItem item2 = createAttributs(child2.getNode(), 2, createChange, 21, null);
        item2.setDeleteChange(createChange);
        descItemRepository.save(item2);

        TestLevelData result = new TestLevelData(item.getDescItemType().getDescItemTypeId(),
            item2.getDescItemType().getDescItemTypeId(), child.getNode().getNodeId(), child2.getNode().getNodeId(), version.getFaVersionId());
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
        ArrFaLevelExt level = response.getBody().as(ArrFaLevelExt.class);

        if (level == null) {
            Assert.fail();
        }
        if (level.getDescItemList().size() != 1) {
            Assert.fail();
        } else {
            ArrDescItemExt descItem = level.getDescItemList().get(0);
            Assert.assertNotNull(descItem.getData());
        }

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, testLevel.getChildNodeId1()).
                parameter(VERSION_ID_ATT, testLevel.getVersionId()).get(GET_LEVEL_URL);
        logger.info(response.asString());
        level = response.getBody().as(ArrFaLevelExt.class);
        if (level == null) {
            Assert.fail();
        }

        if (level.getDescItemList().size() != 1) {
            Assert.fail();
        } else {
            ArrDescItemExt descItem = level.getDescItemList().get(0);
            Assert.assertNotNull(descItem.getData());
            Assert.assertNotNull(descItem.getRecord().getRecord());
        }
    }

    /**
     * Načte archivní pomůcky přes REST volání.
     *
     * @return archivní pomůcky
     */
    private List<ArrFindingAid> getFindingAids() {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<ArrFindingAid> findingAids = Arrays.asList(response.getBody().as(ArrFindingAid[].class));
        return findingAids;
    }

    /**
     * Načte archivní pomůcku přes REST volání.
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid getFindingAid(final Integer findingAidId) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, findingAidId).get(GET_FA_ONE_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        return response.getBody().as(ArrFindingAid.class);
    }

    /**
     * Vytvoří položku archivní pomůcky přes REST volání.
     *
     * @return vytvořená položka
     */
    private ArrFindingAid createFindingAidRest(final String name) {
        RulRuleSet ruleSet = createRuleSet();
        ArrArrangementType arrangementType = createArrangementType();

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_NAME_ATT, name).
                parameter("arrangementTypeId", arrangementType.getArrangementTypeId()).parameter("ruleSetId", ruleSet.getRuleSetId()).
                get(CREATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ArrFindingAid findingAid = response.getBody().as(ArrFindingAid.class);


        return findingAid;
    }

    @Test
    public void testRestCreateDescriptionItem() {

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
        versionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrFaChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrFaLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, true, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, false, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu

        ArrDescItemExt descItem = new ArrDescItemExt();
        descItem.setDescItemType(descItemType);
        descItem.setDescItemSpec(descItemSpec);
        descItem.setData("123");
        descItem.setNode(node);

        ArrDescItem descItemRet = arrangementManager.createDescriptionItem(descItem, version.getFaVersionId());

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

        RulDescItemType descItemType2 = createDescItemType(dataType, true, "ITEM_TYPE2", "Item type 2", "SH3", "Desc 3", false, false, true, 2);
        RulDescItemSpec descItemSpec2 = createDescItemSpec(descItemType2, "ITEM_SPEC2", "Item spec 2", "SH4", "Desc 4", 2);
        createDescItemConstrain(descItemType2, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType2, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu - kontrola position

        descItem = new ArrDescItemExt();
        descItem.setDescItemType(descItemType2);
        descItem.setDescItemSpec(descItemSpec2);
        descItem.setData("123");
        descItem.setNode(node);

        ArrDescItem descItemRet1 = arrangementManager.createDescriptionItem(descItem, version.getFaVersionId());

        descItem = new ArrDescItemExt();
        descItem.setDescItemType(descItemType2);
        descItem.setDescItemSpec(descItemSpec2);
        descItem.setData("1234");
        descItem.setPosition(1);
        descItem.setNode(node);

        ArrDescItem descItemRet2 = arrangementManager.createDescriptionItem(descItem, version.getFaVersionId());

        Assert.assertNotNull(descItemRepository.findOne(descItemRet1.getDescItemId()).getDeleteChange());
        Assert.assertEquals(new Integer(1), descItemRepository.findOne(descItemRet2.getDescItemId()).getPosition());

        descItem = new ArrDescItemExt();
        descItem.setDescItemType(descItemType2);
        descItem.setDescItemSpec(descItemSpec2);
        descItem.setData("12345");
        descItem.setPosition(10);
        descItem.setNode(node);

        ArrDescItem descItemRet3 = arrangementManager.createDescriptionItem(descItem, version.getFaVersionId());

        Assert.assertEquals(new Integer(3), descItemRepository.findOne(descItemRet3.getDescItemId()).getPosition());

    }

    @Test
    public void testRestUpdateDescriptionItem() {

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
        versionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrFaChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrFaLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, true, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, false, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu

        ArrDescItemExt descItem = new ArrDescItemExt();
        descItem.setDescItemType(descItemType);
        descItem.setDescItemSpec(descItemSpec);
        descItem.setData("123");
        descItem.setNode(node);

        ArrDescItem descItemNew = arrangementManager.createDescriptionItem(descItem, version.getFaVersionId());

        // upravení hodnoty bez vytvoření verze

        ArrDescItemExt arrDescItemExt = new ArrDescItemExt();
        BeanUtils.copyProperties(descItemNew, arrDescItemExt);
        arrDescItemExt.setData("124");
        ArrDescItem descItemRet = arrangementManager.updateDescriptionItem(arrDescItemExt ,version.getFaVersionId(), false);

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

        arrDescItemExt = new ArrDescItemExt();
        BeanUtils.copyProperties(descItemNew, arrDescItemExt);
        arrDescItemExt.setData("125");
        descItemRet = arrangementManager.updateDescriptionItem(arrDescItemExt ,version.getFaVersionId(), true);

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

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
        versionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrFaChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrFaLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, true, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 10);

        // přidání hodnot attributů k uzlu

        ArrDescItemExt descItem1 = new ArrDescItemExt();
        descItem1.setDescItemType(descItemType);
        descItem1.setDescItemSpec(descItemSpec);
        descItem1.setData("1");
        descItem1.setNode(node);

        descItem1 = arrangementManager.createDescriptionItem(descItem1, version.getFaVersionId());

        ArrDescItemExt descItem2 = new ArrDescItemExt();
        descItem2.setDescItemType(descItemType);
        descItem2.setDescItemSpec(descItemSpec);
        descItem2.setData("2");
        descItem2.setNode(node);

        descItem2 = arrangementManager.createDescriptionItem(descItem2, version.getFaVersionId());

        ArrDescItemExt descItem3 = new ArrDescItemExt();
        descItem3.setDescItemType(descItemType);
        descItem3.setDescItemSpec(descItemSpec);
        descItem3.setData("3");
        descItem3.setNode(node);

        descItem3 = arrangementManager.createDescriptionItem(descItem3, version.getFaVersionId());

        ArrDescItemExt descItem4 = new ArrDescItemExt();
        descItem4.setDescItemType(descItemType);
        descItem4.setDescItemSpec(descItemSpec);
        descItem4.setData("4");
        descItem4.setNode(node);

        descItem4 = arrangementManager.createDescriptionItem(descItem4, version.getFaVersionId());

        // úprava pozicí

        descItem3.setPosition(1);
        ArrDescItemExt descItem3New = arrangementManager.updateDescriptionItem(descItem3, version.getFaVersionId(), true);

        // kontrola pozice attributu
        checkChangePositionDescItem(descItem1, 2, true);
        checkChangePositionDescItem(descItem2, 3, true);
        checkChangePositionDescItem(descItem3, 1, true);
        checkChangePositionDescItem(descItem4, 4, false);

        descItem3New.setPosition(3);
        ArrDescItemExt descItem3New2 = arrangementManager.updateDescriptionItem(descItem3New, version.getFaVersionId(), true);

        // kontrola pozice attributu
        checkChangePositionDescItem(descItem1, 1, true);
        checkChangePositionDescItem(descItem2, 2, true);
        checkChangePositionDescItem(descItem3, 3, true);
        checkChangePositionDescItem(descItem4, 4, false);

    }

    @Test
    public void testRestSaveDescriptionItems() {
        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
        versionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrFaChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrFaLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, true, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 10);

        // přidání hodnot attributů k uzlu

        ArrDescItemExt descItem1 = new ArrDescItemExt();
        descItem1.setDescItemType(descItemType);
        descItem1.setDescItemSpec(descItemSpec);
        descItem1.setData("1");
        descItem1.setNode(node);

        ArrDescItemExt descItem1Save = arrangementManager.createDescriptionItem(descItem1, version.getFaVersionId());

        ArrDescItemExt descItem2 = new ArrDescItemExt();
        descItem2.setDescItemType(descItemType);
        descItem2.setDescItemSpec(descItemSpec);
        descItem2.setData("2");
        descItem2.setNode(node);

        ArrDescItemExt descItem2Save = arrangementManager.createDescriptionItem(descItem2, version.getFaVersionId());

        ArrDescItemExt descItem3 = new ArrDescItemExt();
        descItem3.setDescItemType(descItemType);
        descItem3.setDescItemSpec(descItemSpec);
        descItem3.setData("3");
        descItem3.setNode(node);

        ArrDescItemExt descItem3Save = arrangementManager.createDescriptionItem(descItem3, version.getFaVersionId());

        ArrDescItemExt descItem4 = new ArrDescItemExt();
        descItem4.setDescItemType(descItemType);
        descItem4.setDescItemSpec(descItemSpec);
        descItem4.setData("4");
        descItem4.setNode(node);

        ArrDescItemExt descItem4Save = arrangementManager.createDescriptionItem(descItem4, version.getFaVersionId());

        // vytvoření změn k odeslání

        ArrDescItemSavePack descItemSavePack = new ArrDescItemSavePack();

        List<ArrDescItemExt> descItems = new ArrayList<>();
        List<ArrDescItemExt> deleteDescItems = new ArrayList<>();

        deleteDescItems.add(descItem1Save);

        descItem2Save.setPosition(1);
        descItems.add(descItem2Save);

        descItem3Save.setPosition(2);
        descItems.add(descItem3Save);

        ArrDescItemExt descItemNew1 = new ArrDescItemExt();
        descItemNew1.setDescItemType(descItemType);
        descItemNew1.setDescItemSpec(descItemSpec);
        descItemNew1.setData("11");
        descItemNew1.setNode(node);
        descItemNew1.setPosition(3);
        descItems.add(descItemNew1);

        descItem4Save.setPosition(4);
        descItems.add(descItem4Save);

        ArrDescItemExt descItemNew2 = new ArrDescItemExt();
        descItemNew2.setDescItemType(descItemType);
        descItemNew2.setDescItemSpec(descItemSpec);
        descItemNew2.setData("12");
        descItemNew2.setNode(node);
        descItemNew2.setPosition(5);
        descItems.add(descItemNew2);

        descItemSavePack.setCreateNewVersion(true);
        descItemSavePack.setFaVersionId(version.getFaVersionId());
        descItemSavePack.setDescItems(descItems);
        descItemSavePack.setDeleteDescItems(deleteDescItems);

        List<ArrDescItemExt> descItemListSave = arrangementManager.saveDescriptionItems(descItemSavePack);

        Assert.assertNotNull(descItemListSave);
        Assert.assertEquals(6, descItemListSave.size());

        for (ArrDescItemExt descItem : descItemListSave) {
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
    private void checkChangePositionDescItem(ArrDescItemExt descItem, int newPosition, boolean hasNewRecord) {
        List<ArrDescItem> descItemList1 = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItem.getDescItemObjectId());
        if (descItemList1.size() != 1) {
            Assert.fail("Nesprávný počet položek");
        }
        ArrDescItem descItemChange = descItemList1.get(0);
        if (hasNewRecord) {
            Assert.assertNotEquals("Nemůže být stejný záznam, protože se provedla změna pozice", descItem, descItemChange);
        } else {
            Assert.assertEquals("Musí být stejný záznam, protože se neprovedla změna pozice", descItem, descItemChange);
        }
        Assert.assertEquals(newPosition, descItemChange.getPosition().intValue());
    }

    @Test
    public void testRestDeleteDescriptionItem() {

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);

        ArrFaVersion version = createFindingAidVersion(findingAid, null, false);
        ArrFaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootFaLevel(parent);
        versionRepository.save(version);
        LocalDateTime startTime = version.getCreateChange().getChangeDate();

        ArrFaChange createChange = createFaChange(startTime.minusSeconds(1));
        ArrFaLevel faLevel = createLevel(2, parent, createChange);
        levelRepository.save(faLevel);

        ArrNode node = faLevel.getNode();

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);
        Assert.assertNotNull("Neexistuje záznam pro datový typ INTEGER", dataType);

        // vytvoření závislých dat

        RulDescItemType descItemType = createDescItemType(dataType, true, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, version, false, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, version, null, null, 50);

        // přidání hodnoty attributu

        ArrDescItemExt descItem = new ArrDescItemExt();
        descItem.setDescItemType(descItemType);
        descItem.setDescItemSpec(descItemSpec);
        descItem.setData("123");
        descItem.setNode(node);

        ArrDescItem descItemNew = arrangementManager.createDescriptionItem(descItem, version.getFaVersionId());

        // smazání hodnoty attributu

        /*ArrDescItem descItemDel = arrangementManager.deleteDescriptionItem(descItemNew);

        Assert.assertEquals(descItemNew, descItemDel);

        Assert.assertNotNull(descItemDel.getDeleteChange());*/

    }

}
