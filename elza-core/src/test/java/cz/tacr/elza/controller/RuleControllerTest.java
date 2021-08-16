package cz.tacr.elza.controller;


import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.RulExportFilterVO;
import cz.tacr.elza.controller.vo.RulOutputFilterVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementExtensionRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static cz.tacr.elza.repository.ExceptionThrow.ruleSet;


/**
 * Test for rule controller
 */
public class RuleControllerTest extends AbstractControllerTest {

    @Autowired
    private ArrangementExtensionRepository arrExtsRepo;

    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;

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
    public void visiblePolicy() throws ApiException {
        Fund test = createFund("Test", null);
        ArrFundVersionVO fundVersion = getOpenVersion(test);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        TreeNodeVO rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        List<RulPolicyTypeVO> policyAllTypes = getAllPolicyTypes();
        Assert.assertNotNull(policyAllTypes);
        Assert.assertTrue(policyAllTypes.size()==4);

        List<RulPolicyTypeVO> policyTypes = getPolicyTypes(fundVersion.getId());
        Assert.assertNotNull(policyTypes);

        // check that none is set to node
        RuleController.VisiblePolicyTypes visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId());
        Assert.assertNotNull(visiblePolicy);
        Map<Integer, Boolean> policyTypeIdsMap = visiblePolicy.getNodePolicyTypeIdsMap();
        Assert.assertNotNull(policyTypeIdsMap);
        Assert.assertTrue(policyTypeIdsMap.size() == 0);

        // set new policies
        RuleController.VisiblePolicyParams params = new RuleController.VisiblePolicyParams();
        Map<Integer, Boolean> policyTypeIdsMapSet = new HashMap<>();
        policyTypes.stream().forEach(item -> policyTypeIdsMapSet.put(item.getId(), false));
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapSet);
        params.setNodeExtensions(new HashSet<>());
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        // check map again
        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId());
        Assert.assertNotNull(visiblePolicy);
        policyTypeIdsMap = visiblePolicy.getNodePolicyTypeIdsMap();
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
        params.setNodeExtensions(new HashSet<>());
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId());
        Assert.assertNotNull(visiblePolicy);
        policyTypeIdsMap = visiblePolicy.getNodePolicyTypeIdsMap();
        Assert.assertNotNull(policyTypeIdsMap);
        Assert.assertTrue(policyTypeIdsMap.size() == 0);

        final RulArrangementExtension ext1 = getExtension("01", fundVersion);
        final RulArrangementExtension ext2 = getExtension("02", fundVersion);

        // NodeExtensions
        // TODO odstranit po funkčním balíčku s rozšířeními
        params = new RuleController.VisiblePolicyParams();
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapDelete);
        final HashSet<Integer> ids = new HashSet<>(2);
        ids.add(ext1.getArrangementExtensionId());
        ids.add(ext2.getArrangementExtensionId());
        params.setNodeExtensions(ids);
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId());
        Assert.assertTrue(visiblePolicy.getNodeExtensions().size() == 2);


        params = new RuleController.VisiblePolicyParams();
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapDelete);
        ids.remove(ext1.getArrangementExtensionId());
        params.setNodeExtensions(ids);
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId());
        Assert.assertTrue(visiblePolicy.getNodeExtensions().size() == 1);
        Assert.assertTrue(visiblePolicy.getNodeExtensions().get(0).getId().equals(ext2.getArrangementExtensionId()));

        params = new RuleController.VisiblePolicyParams();
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapDelete);
        params.setNodeExtensions(new HashSet<>());
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId());
        Assert.assertTrue(visiblePolicy.getNodeExtensions().size() == 0);

        Assert.assertTrue(nodeExtensionRepository.count() == 2);
        nodeExtensionRepository.deleteAll();
        arrExtsRepo.delete(ext1);
        arrExtsRepo.delete(ext2);
        // TODO END
    }


    // TODO odstranit po funkčním balíčku s rozšířeními
    private RulArrangementExtension getExtension(String baseName, ArrFundVersionVO fundVersion) {
        final RulArrangementExtension extension = new RulArrangementExtension();
        extension.setCode(baseName);
        extension.setName(baseName);
        RulRuleSet ruleSet = ruleSetRepository.findById(fundVersion.getRuleSetId())
                .orElseThrow(ruleSet(fundVersion.getRuleSetId()));
        extension.setRulPackage(ruleSet.getPackage());
        extension.setRuleSet(ruleSet);
        return arrExtsRepo.save(extension);
    }

    @Test
    public void getOutputFiltersTest() {
        List<RulOutputFilterVO> rulOutputFilterVOS = getOutputFilters();

        Assert.assertEquals(1, rulOutputFilterVOS.size());

        Assert.assertEquals("SRD_TEST_OUTPUT_FILTER", rulOutputFilterVOS.get(0).getCode());
        Assert.assertEquals("SRD_TEST_OUTPUT_FILTER.yaml", rulOutputFilterVOS.get(0).getFilename());
    }

    @Test
    public void getExportFiltersTest() {
        List<RulExportFilterVO> rulExportFilterVOS = getExportFilters();

        Assert.assertEquals(1, rulExportFilterVOS.size());

        Assert.assertEquals("SRD_TEST_EXPORT_FILTER", rulExportFilterVOS.get(0).getCode());
        Assert.assertEquals("SRD_TEST_EXPORT_FILTER.yaml", rulExportFilterVOS.get(0).getFilename());
    }

    @Test
    public void getItemTypesByRuleSetTest() {
        List<RulRuleSetVO> ruleSetVOList = getRuleSets();
        RulRuleSetVO ruleSetVO = null;

        for (RulRuleSetVO rulRuleSetVO : ruleSetVOList) {
            if (rulRuleSetVO.getCode().equals("SIMPLE-DEV")) {
                ruleSetVO = rulRuleSetVO;
                break;
            }
        }

        List<String> itemTypeCodes = getItemTypeCodesByRuleSet(ruleSetVO);
        Assert.assertEquals(3, itemTypeCodes.size());
    }
}
