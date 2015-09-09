package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrFaLevelWithExtraNode;

/**
 * Kompletní test {@link ArrangementManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 9. 2015
 */
public class ArrangementManagerUsecaseTest extends AbstractRestTest {

    private RulRuleSet ruleSet;
    private ArrArrangementType arrangementType;

    /** Příprava dat. */
    @Override
    @Before
    public void setUp() {
        super.setUp();
        ruleSet = createRuleSet();
        arrangementType = createArrangementType();
    }

    @Test
    public void UsecaserTest() {
        ArrFindingAid findingAid = testCreateFindingAid();
        findingAid = testUpdateFindingAid(findingAid);
        testApproveFindingAidVersion(findingAid);
        testAddLevel(findingAid);
    }

    /**
     * Otestuje vytváření uzlů.
     *
     * @param findingAid archivní pomůcka
     */
    private void testAddLevel(ArrFindingAid findingAid) {
        ArrFaVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode rootNode = version.getRootFaLevel().getNode();
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(version.getRootFaLevel());

        // přidání prvního levelu pod root
        ArrFaLevelWithExtraNode childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrFaLevel child1 = childLevelWithExtraNode.getFaLevel();
        Assert.notNull(child1);
        Assert.isTrue(child1.getPosition().equals(1));
        Assert.isTrue(child1.getParentNode().equals(rootNode));

        List<ArrFaLevel> children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 1);
        Assert.isTrue(children.get(0).getFaLevelId().equals(child1.getFaLevelId()));

        //přidání druhého levelu pod root
        levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(getLevelByNodeId(rootNode.getNodeId()));

        childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrFaLevel child2 = childLevelWithExtraNode.getFaLevel();
        Assert.notNull(child2);
        Assert.isTrue(child2.getPosition().equals(2));
        Assert.isTrue(child2.getParentNode().equals(rootNode));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 2);
        Assert.isTrue(children.get(0).getFaLevelId().equals(child1.getFaLevelId()));
        Assert.isTrue(children.get(1).getFaLevelId().equals(child2.getFaLevelId()));

        //přidání třetího levelu na první pozici pod root
        levelWithExtraNode = new ArrFaLevelWithExtraNode();
        child1 = getLevelByNodeId(child1.getNode().getNodeId());
        levelWithExtraNode.setFaLevel(child1);
        levelWithExtraNode.setExtraNode(child1.getParentNode());

        childLevelWithExtraNode = createLevelBefore(levelWithExtraNode);
    }

    /**
     * Najde podřízené úrovně.
     *
     * @param rootNode nadřazený uzel pro který hledáme potomky
     * @param version verze, může být null
     *
     * @return potomky předaného uzlu
     */
    private List<ArrFaLevel> getSubLevels(ArrNode rootNode, ArrFaVersion version) {
        Response response;
        if (version == null) {
            response = get(spec -> spec.parameter(NODE_ID_ATT, rootNode.getNodeId()), FIND_SUB_LEVELS_URL);
        } else {
            response = get(spec -> spec.parameter(NODE_ID_ATT, rootNode.getNodeId())
                    .parameter(VERSION_ID_ATT, version.getFaVersionId()), FIND_SUB_LEVELS_URL);
        }

        return Arrays.asList(response.getBody().as(ArrFaLevel[].class));
    }

    /**
     * Vytvoří nový uzel pod předaným uzlem.
     *
     * @param levelWithExtraNode rodičovský uzel
     *
     * @return nový uzel
     */
    private ArrFaLevelWithExtraNode createLevelChild(ArrFaLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_CHILD_URL);
        ArrFaLevelWithExtraNode parent = response.getBody().as(ArrFaLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Vytvoří nový uzel před předaným uzlem.
     *
     * @param levelWithExtraNode uzal před kterým se vytvoří nový uzel
     *
     * @return nový uzel
     */
    private ArrFaLevelWithExtraNode createLevelBefore(ArrFaLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_BEFORE_URL);
        ArrFaLevelWithExtraNode parent = response.getBody().as(ArrFaLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Uzavření verze archivní pomůcky a následné kontroly.
     *
     * @param findingAid archivní pomůcka
     */
    private void testApproveFindingAidVersion(ArrFindingAid findingAid) {
        ArrFaVersion openVersion = getFindingAidOpenVersion(findingAid);

        ArrFaVersion newOpenVersion = approveVersion(openVersion);
        Assert.notNull(newOpenVersion);
        Assert.isTrue(!openVersion.getFaVersionId().equals(newOpenVersion.getFaVersionId()));
        Assert.isTrue(newOpenVersion.getArrangementType().getArrangementTypeId().equals(arrangementType.getArrangementTypeId()));
        Assert.isTrue(newOpenVersion.getRuleSet().getRuleSetId().equals(ruleSet.getRuleSetId()));
        Assert.isTrue(newOpenVersion.getLockChange() == null);

        ArrFaVersion newOpenVersionCheck = getFindingAidOpenVersion(findingAid);
        Assert.isTrue(newOpenVersionCheck.equals(newOpenVersion));

        ArrFaVersion closedVersion = getVersionById(openVersion.getFaVersionId());
        Assert.notNull(closedVersion);
        Assert.isTrue(closedVersion.getFaVersionId().equals(openVersion.getFaVersionId()));
        Assert.isTrue(closedVersion.getLockChange() != null);

        List<ArrFaVersion> versions = getFindingAidVersions(findingAid);
        Assert.isTrue(versions.size() == 2);
        Assert.isTrue(versions.get(0).getLockChange() != null);
        Assert.isTrue(versions.get(1).getLockChange() == null);

        approveVersionWithError(closedVersion);
    }

    /**
     * Načte všechny verze archivní pomůcky.
     *
     * @param findingAid archivní pomůcka
     *
     * @return seznam verzí archivní pomůcky
     */
    private List<ArrFaVersion> getFindingAidVersions(ArrFindingAid findingAid) {
        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_FINDING_AID_VERSIONS_URL);

        return Arrays.asList(response.getBody().as(ArrFaVersion[].class));
    }

    /**
     * Načte verzi archivní pomůcky přes REST volání.
     *
     * @param versionId id verze
     *
     * @return verze archivní pomůcky
     */
    private ArrFaVersion getVersionById(Integer versionId) {
        Response response = get(spec -> spec.parameter(VERSION_ID_ATT, versionId), GET_VERSION_ID_URL);

        return response.getBody().as(ArrFaVersion.class);
    }

    /**
     * Uzavře verzi archivní pomůcky.
     *
     * @param openVersion otevřená verze archivní pomůcky
     *
     * @return nová otevřená verze archivní pomůcky
     */
    private ArrFaVersion approveVersion(ArrFaVersion openVersion) {
        Response response = put(spec -> spec.body(openVersion).
                parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId())
                , APPROVE_VERSION_URL);

        return response.getBody().as(ArrFaVersion.class);
    }

    /**
     * Pokusí se uzavřít verzi archivní pomůcky. Očekává chybu.
     *
     * @param openVersion otevřená verze archivní pomůcky
     *
     * @return nová otevřená verze archivní pomůcky
     */
    private void approveVersionWithError(ArrFaVersion openVersion) {
        put(spec -> spec.body(openVersion).
                parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId())
                , APPROVE_VERSION_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Aktualizace archivní pomůcky a následné kontroly.
     *
     * @param findingAid archivní pomůcka
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid testUpdateFindingAid(ArrFindingAid findingAid) {
        ArrFindingAid updatedFindingAid = updateFindingAid(findingAid, TEST_UPDATE_NAME);
        testChangedFindingAid(updatedFindingAid, TEST_UPDATE_NAME, 1);

        return updatedFindingAid;
    }

    /**
     * Ajtualizace archivní pomůcky.
     *
     * @return aktualizovaná archivní pomůcka
     */
    private ArrFindingAid updateFindingAid(ArrFindingAid findingAid, String testUpdateName) {
        findingAid.setName(testUpdateName);
        Response response = put(spec -> spec.body(findingAid), UPDATE_FA_URL);

        return response.getBody().as(ArrFindingAid.class);
    }

    /**
     * Vytvoření a kontrola archivní pomůcky.
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid testCreateFindingAid() {
        ArrFindingAid findingAid = createFindingAid();
        testChangedFindingAid(findingAid, TEST_NAME, 1);

        return findingAid;
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param findingAid archivní pomůcka
     * @param testName předpokládaný název archivní pomůcky
     * @param findingAidsCount předpokládaný počet archivních pomůcek
     */
    private void testChangedFindingAid(ArrFindingAid findingAid, String testName, int findingAidsCount) {
        Assert.notNull(findingAid);
        Assert.notNull(findingAid.getFindingAidId());

        Integer findingAidId = findingAid.getFindingAidId();
        findingAid = getFindingAid(findingAidId);

        Assert.notNull(findingAid);
        Assert.notNull(findingAid.getFindingAidId());
        Assert.isTrue(findingAid.getFindingAidId().equals(findingAidId));
        Assert.isTrue(findingAid.getName().equals(testName));

        List<ArrFindingAid> findingAids = getFindingAids();
        Assert.isTrue(findingAids.size() == findingAidsCount);

        ArrFaVersion openVersion = getFindingAidOpenVersion(findingAid);
        Assert.notNull(openVersion);
        Assert.isNull(openVersion.getLockChange());

        Integer nodeId = openVersion.getRootFaLevel().getNode().getNodeId();
        ArrFaLevelExt rootLevel = getLevelByNodeId(nodeId);
        Assert.notNull(rootLevel);
        Assert.isTrue(rootLevel.getNode().getNodeId().equals(nodeId));
        Assert.isNull(rootLevel.getParentNode());
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid createFindingAid() {
        Response response = put(spec -> spec.parameter(FA_NAME_ATT, TEST_NAME)
                .parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId())
                .parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId()), CREATE_FA_URL);

        ArrFindingAid findingAid = response.getBody().as(ArrFindingAid.class);

        return findingAid;
    }
}
