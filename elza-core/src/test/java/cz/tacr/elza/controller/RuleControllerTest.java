package cz.tacr.elza.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;


/**
 * Test for rule controller
 */
public class RuleControllerTest extends AbstractControllerTest {

    @Test
    public void getDataTypesTest() {
        getDataTypes();
    }

    @Test
    public void getDescItemTypesTest() {
        getDescItemTypes();
    }

    @Test
    public void getTemplatesTest() {
        getTemplates();
    }

    @Test
    public void getPackagesTest() {
        getPackages();
    }

    @Test
    public void getRuleSetsTest() {
        getRuleSets();
    }

    @Test
    public void visiblePolicy() {
        ArrFundVO test = createFund("Test", null);
        ArrFundVersionVO fundVersion = getOpenVersion(test);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        TreeNodeClient rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        List<RulPolicyTypeVO> policyAllTypes = getAllPolicyTypes();
        Assert.assertNotNull(policyAllTypes);
        Assert.assertTrue(policyAllTypes.size()==4);

        List<RulPolicyTypeVO> policyTypes = getPolicyTypes(fundVersion.getId());
        Assert.assertNotNull(policyTypes);

        // check that all are visible by default
        RuleController.VisiblePolicyTypes visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId(), false);
        Assert.assertNotNull(visiblePolicy);
        Map<Integer, Boolean> policyTypeIdsMap = visiblePolicy.getPolicyTypeIdsMap();
        Assert.assertNotNull(policyTypeIdsMap);
        Assert.assertTrue(policyTypeIdsMap.size() == 4);

        for (RulPolicyTypeVO policyType : policyTypes) {
            Boolean state = policyTypeIdsMap.get(policyType.getId());
            Assert.assertNotNull(state);
            Assert.assertTrue(state);
        }

        RuleController.VisiblePolicyParams params = new RuleController.VisiblePolicyParams();
        Map<Integer, Boolean> policyTypeIdsMapSet = new HashMap<>();
        policyTypes.stream().forEach(item -> policyTypeIdsMapSet.put(item.getId(), false));
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapSet);
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId(), false);
        Assert.assertNotNull(visiblePolicy);
        policyTypeIdsMap = visiblePolicy.getPolicyTypeIdsMap();
        Assert.assertNotNull(policyTypeIdsMap);
        Assert.assertTrue(policyTypeIdsMap.size() == 4);

        // check that all are false
        for (RulPolicyTypeVO policyType : policyTypes) {
            Boolean state = policyTypeIdsMap.get(policyType.getId());
            Assert.assertNotNull(state);
            Assert.assertTrue(!state);
        }

        // drop old settings
        params = new RuleController.VisiblePolicyParams();
        Map<Integer, Boolean> policyTypeIdsMapDelete = new HashMap<>();
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapDelete);
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId(), false);
        Assert.assertNotNull(visiblePolicy);
        policyTypeIdsMap = visiblePolicy.getPolicyTypeIdsMap();
        Assert.assertNotNull(policyTypeIdsMap);
        Assert.assertTrue(policyTypeIdsMap.size() == 4);

        // check that all are true
        for (RulPolicyTypeVO policyType : policyTypes) {
            Boolean state = policyTypeIdsMap.get(policyType.getId());
            Assert.assertNotNull(state);
            Assert.assertTrue(state);
        }
    }

}
