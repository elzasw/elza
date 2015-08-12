package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * Testy pro {@link ArrangementManager}.
 */
public class ArrangementManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CREATE_FA_URL = ARRANGEMENT_MANAGER_URL + "/createFindingAid";
    private static final String UPDATE_FA_URL = ARRANGEMENT_MANAGER_URL + "/updateFindingAid";
    private static final String DELETE_FA_URL = ARRANGEMENT_MANAGER_URL + "/deleteFindingAid";
    private static final String GET_FA_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAids";
    private static final String GET_FA_ONE_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAid";
    private static final String GET_ARRANGEMENT_TYPES_URL = ARRANGEMENT_MANAGER_URL + "/getArrangementTypes";
    private static final String GET_FINDING_AID_VERSIONS_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAidVersions";
    private static final String APPROVE_VERSION_URL = ARRANGEMENT_MANAGER_URL + "/approveVersion";
    private static final String GET_VERSION_ID_URL = ARRANGEMENT_MANAGER_URL + "/getVersion";
    private static final String GET_VERSION_BY_FA_ID_URL = ARRANGEMENT_MANAGER_URL + "/getOpenVersionByFindingAidId";
    private static final String FIND_SUB_LEVELS_URL = ARRANGEMENT_MANAGER_URL + "/findSubLevels";

    private static final String ADD_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/addLevel";
    private static final String ADD_LEVEL_AFTER_URL = ARRANGEMENT_MANAGER_URL + "/addLevelAfter";
    private static final String ADD_LEVEL_CHILD_URL = ARRANGEMENT_MANAGER_URL + "/addLevelChild";
    private static final String MOVE_LEVEL_UNDER_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelUnder";
    private static final String MOVE_LEVEL_AFTER_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelAfter";
    private static final String DELETE_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/deleteLevel";
    private static final String FIND_LEVEL_BY_NODE_ID_URL = ARRANGEMENT_MANAGER_URL + "/findLevelByNodeId";

    private static final String FA_NAME_ATT = "name";
    private static final String FA_ID_ATT = "findingAidId";
    private static final String ARRANGEMENT_TYPE_ID_ATT = "arrangementTypeId";
    private static final String RULE_SET_ID_ATT = "ruleSetId";
    private static final String NODE_ID_ATT = "nodeId";
    private static final String PARENT_NODE_ID_ATT = "parentNodeId";
    private static final String PREDECESSOR_NODE_ID_ATT = "predecessorNodeId";
    private static final String VERSION_ID_ATT = "versionId";

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
        FindingAid findingAid = createFindingAid(TEST_NAME);

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
        FindingAid findingAid = createFindingAid(TEST_NAME);

        arrangementManager.updateFindingAid(findingAid.getFindingAidId(), TEST_UPDATE_NAME);
    }

    // ---- REST test ----
    @Test
    public void testRestCreateFindingAid() throws Exception {
        FindingAid findingAid = createFindingAidRest(TEST_NAME);

        Assert.assertNotNull(findingAid);
        Assert.assertNotNull(findingAid.getFindingAidId());
    }

    @Test
    public void testRestGetFindingAid() throws Exception {
        FindingAid findingAid = createFindingAidRest(TEST_NAME);

        FindingAid getFindingAid = getFindingAid(findingAid.getFindingAidId());

        Assert.assertNotNull(getFindingAid);
        Assert.assertEquals(TEST_NAME, getFindingAid.getName());
        Assert.assertEquals(findingAid.getFindingAidId(), getFindingAid.getFindingAidId());
    }

    @Test
    public void testRestGetFindingAids() throws Exception {
        createFindingAidRest(TEST_NAME);

        List<FindingAid> findingAids = getFindingAids();
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
        FindingAid findingAid = createFindingAidRest(TEST_NAME);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);

        List<FaVersion> versions = Arrays.asList(response.getBody().as(FaVersion[].class));
        FaVersion version = versions.iterator().next();

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

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, idFinfingAid)
                .parameter(FA_NAME_ATT, TEST_UPDATE_NAME)
                .get(UPDATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FindingAid updatedFindingAid = null;

        List<FindingAid> findingAids = getFindingAids();
        for (FindingAid findingAid : findingAids) {
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
        ArrangementType arrangementType = new ArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_ARRANGEMENT_TYPES_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<ArrangementType> arrangementTypes = Arrays.asList(response.getBody().as(ArrangementType[].class));
        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !arrangementTypes.isEmpty());
    }

    @Test
    public void testRestGetFindingAidVersions() throws Exception {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        int versionCount = 10;
        for (int i = 0; i < versionCount; i++) {
            createFindingAidVersion(findingAid, false);
        }

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<FaVersion> versions = Arrays.asList(response.getBody().as(FaVersion[].class));
        Assert.assertTrue(versions.size() == versionCount + 1);

        FaVersion prevVersion = null;
        for (FaVersion version : versions) {
            if (prevVersion == null) {
                prevVersion = version;
                continue;
            }

            if (prevVersion.getCreateChange().getChangeDate().isBefore(version.getCreateChange().getChangeDate())) {
                Assert.fail();
            }
        }
    }

    @Test
    public void testRestApproveVersion() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);

        List<FaVersion> versions = Arrays.asList(response.getBody().as(FaVersion[].class));
        FaVersion version = versions.iterator().next();

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).
                parameter(ARRANGEMENT_TYPE_ID_ATT, version.getArrangementType().getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, version.getRuleSet().getRuleSetId()).
                get(APPROVE_VERSION_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaVersion newVersion = response.getBody().as(FaVersion.class);
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_FINDING_AID_VERSIONS_URL);

        versions = Arrays.asList(response.getBody().as(FaVersion[].class));

        Assert.assertNotNull(newVersion);
        Assert.assertTrue(newVersion.getLockChange() == null);
        Assert.assertTrue(versions.size() == 2);
    }

    @Test
    public void testRestGetVersionByFa() throws Exception {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = createFindingAidVersion(findingAid, true);
        // prvni version se vytvori pri zalozeni FA
        Integer createVersionId = version.getFaVersionId() - 1;
        FaVersion versionChange = createFindingAidVersion(findingAid, true);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(VERSION_ID_ATT, version.getFaVersionId()).get(GET_VERSION_ID_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        FaVersion resultVersion = response.getBody().as(FaVersion.class);
        Assert.assertNotNull("Version nebylo nalezeno", resultVersion);
        Assert.assertEquals(resultVersion.getFaVersionId(), version.getFaVersionId());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(FA_ID_ATT, findingAid.getFindingAidId()).get(GET_VERSION_BY_FA_ID_URL);
        logger.info(response.asString());

        Assert.assertEquals(200, response.statusCode());

        resultVersion = response.getBody().as(FaVersion.class);
        Assert.assertNotNull("Version nebylo nalezeno", resultVersion);
        Assert.assertEquals(resultVersion.getFaVersionId(), createVersionId);
        Assert.assertNull(resultVersion.getLockChange());
    }

    @Test
    public void testRestGetLevelByParent() throws Exception {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = createFindingAidVersion(findingAid, null, false);
        FaLevel parent = createLevel(1, null, version.getCreateChange());
        version.setRootNode(parent);
        versionRepository.save(version);

        FaLevel child = createLevel(2, parent, version.getCreateChange());
        FaLevel child2 = createLevel(2, parent, version.getCreateChange());
        FaChange change = createFaChange(LocalDateTime.now());
        child2.setDeleteChange(change);
        levelRepository.save(child2);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, parent.getNodeId()).
                parameter(VERSION_ID_ATT, version.getFaVersionId()).get(FIND_SUB_LEVELS_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        List<FaLevel> levelList = Arrays.asList(response.getBody().as(FaLevel[].class));
        if (levelList.size() != 1) {
            Assert.fail();
        }

        FaChange lockChange = createFaChange(LocalDateTime.now());
        version.setLockChange(lockChange);
        versionRepository.save(version);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                parameter(NODE_ID_ATT, parent.getNodeId()).
                parameter(VERSION_ID_ATT, version.getFaVersionId()).get(FIND_SUB_LEVELS_URL);
        logger.info(response.asString());
        levelList = Arrays.asList(response.getBody().as(FaLevel[].class));
        if (levelList.size() != 1) {
            Assert.fail();
        }
    }
    //----
    @Test
    public void testRestAddLevel() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        List<FaLevel> subLevels = arrangementManager.findSubLevels(version.getRootNode().getNodeId(), version.getFaVersionId());
        Assert.assertTrue(subLevels.isEmpty());

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        subLevels = arrangementManager.findSubLevels(version.getRootNode().getNodeId(), version.getFaVersionId());
        Assert.assertTrue(subLevels.size() == 1);

        FaLevel child = response.getBody().as(FaLevel.class);
        Assert.assertTrue(child.getFaLevelId().equals(subLevels.iterator().next().getFaLevelId()));
    }

    @Test
    public void testRestAddLevelAfter() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel first = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, first.getNodeId()).put(ADD_LEVEL_AFTER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel second = response.getBody().as(FaLevel.class);

        List<FaLevel> subLevels = arrangementManager.findSubLevels(version.getRootNode().getNodeId(), version.getFaVersionId());
        Assert.assertTrue(subLevels.size() == 2);

        Iterator<FaLevel> iterator = subLevels.iterator();
        Assert.assertTrue(first.getFaLevelId().equals(iterator.next().getFaLevelId()));
        Assert.assertTrue(second.getFaLevelId().equals(iterator.next().getFaLevelId()));
    }

    @Test
    public void testRestAddLevelChild() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel parent = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, parent.getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel child = response.getBody().as(FaLevel.class);

        List<FaLevel> subLevels = arrangementManager.findSubLevels(parent.getNodeId(), version.getFaVersionId());
        Assert.assertTrue(subLevels.size() == 1);

        Assert.assertTrue(child.getFaLevelId().equals(subLevels.iterator().next().getFaLevelId()));
        Assert.assertTrue(child.getParentNodeId().equals(parent.getNodeId()));
    }

    @Test
    public void testRestMoveLevelUnder() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel first = response.getBody().as(FaLevel.class);

        response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel second = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, first.getNodeId()).
                parameter(PARENT_NODE_ID_ATT, second.getNodeId()).put(MOVE_LEVEL_UNDER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel child = response.getBody().as(FaLevel.class);
        Assert.assertTrue(child.getParentNodeId().equals(second.getNodeId()));

        List<FaLevel> subLevels = arrangementManager.findSubLevels(second.getNodeId(), version.getFaVersionId());
        Assert.assertTrue(subLevels.size() == 1);
        Assert.assertTrue(child.getFaLevelId().equals(subLevels.iterator().next().getFaLevelId()));
    }

    @Test
    public void testRestMoveLevelAfter() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        FaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel parent = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, parent.getNodeId()).put(ADD_LEVEL_CHILD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel child = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, child.getNodeId()).
                parameter(PREDECESSOR_NODE_ID_ATT, parent.getNodeId()).put(MOVE_LEVEL_AFTER_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel movedChild = response.getBody().as(FaLevel.class);

        List<FaLevel> subLevels = arrangementManager.findSubLevels(version.getRootNode().getNodeId(), version.getFaVersionId());

        Assert.assertTrue(subLevels.size() == 2);
        Assert.assertTrue(movedChild.getParentNodeId().equals(parent.getParentNodeId()));
    }

    @Test
    public void testRestDeleteLevel() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel node = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, node.getNodeId()).put(DELETE_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel deletedNode = response.getBody().as(FaLevel.class);

        Assert.assertTrue(deletedNode.getDeleteChange() != null);
        Assert.assertTrue(node.getNodeId().equals(deletedNode.getNodeId()));
        Assert.assertTrue(node.getFaLevelId().equals(deletedNode.getFaLevelId()));
    }

    @Test
    public void testRestFindLevelByNodeId() {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        Response response = given().parameter(FA_ID_ATT, findingAid.getFindingAidId()).put(ADD_LEVEL_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel node = response.getBody().as(FaLevel.class);

        response = given().parameter(NODE_ID_ATT, node.getNodeId()).get(FIND_LEVEL_BY_NODE_ID_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FaLevel foundNode = response.getBody().as(FaLevel.class);

        Assert.assertTrue(node.getNodeId().equals(foundNode.getNodeId()));
        Assert.assertTrue(node.getFaLevelId().equals(foundNode.getFaLevelId()));
    }

    /**
     * Načte archivní pomůcky přes REST volání.
     *
     * @return archivní pomůcky
     */
    private List<FindingAid> getFindingAids() {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<FindingAid> findingAids = Arrays.asList(response.getBody().as(FindingAid[].class));
        return findingAids;
    }

    /**
     * Načte archivní pomůcku přes REST volání.
     *
     * @return archivní pomůcka
     */
    private FindingAid getFindingAid(final Integer findingAidId) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, findingAidId).get(GET_FA_ONE_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        return response.getBody().as(FindingAid.class);
    }

    /**
     * Vytvoří položku archivní pomůcky přes REST volání.
     *
     * @return vytvořená položka
     */
    private FindingAid createFindingAidRest(final String name) {
        RuleSet ruleSet = createRuleSet();
        ArrangementType arrangementType = createArrangementType();

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_NAME_ATT, name).
                parameter("arrangementTypeId", arrangementType.getId()).parameter("ruleSetId", ruleSet.getId()).
                get(CREATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FindingAid findingAid = response.getBody().as(FindingAid.class);


        return findingAid;
    }
}