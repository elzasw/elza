package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Petr Compel
 * @since 17.2.2016
 */
public class RuleControllerTest extends AbstractControllerTest {

    private static final String IMPORT_PACKAGE = RULE_CONTROLLER_URL + "/importPackage";
    private static final String DELETE_PACKAGE = RULE_CONTROLLER_URL + "/deletePackage/{code}";
    private static final String EXPORT_PACKAGE = RULE_CONTROLLER_URL + "/exportPackage/{code}";

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
    public void deleteImportExportPackageTest() throws Exception {
        deletePackage(getPackages().get(0).getCode());
        importPackage();
        exportPackage(getPackages().get(0).getCode());
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
        Assert.notEmpty(policyAllTypes);

        List<RulPolicyTypeVO> policyTypes = getPolicyTypes(fundVersion.getId());
        Assert.notEmpty(policyTypes);

        RuleController.VisiblePolicyTypes visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId(), false);
        Assert.notNull(visiblePolicy);
        Map<Integer, Boolean> policyTypeIdsMap = visiblePolicy.getPolicyTypeIdsMap();
        Assert.notNull(policyTypeIdsMap);
        Assert.isTrue(policyTypeIdsMap.size() == 3);

        for (RulPolicyTypeVO policyType : policyTypes) {
            Boolean state = policyTypeIdsMap.get(policyType.getId());
            Assert.notNull(state);
            Assert.isTrue(state);
        }

        RuleController.VisiblePolicyParams params = new RuleController.VisiblePolicyParams();
        Map<Integer, Boolean> policyTypeIdsMapSet = new HashMap<>();
        policyTypes.stream().forEach(item -> policyTypeIdsMapSet.put(item.getId(), false));
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapSet);
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId(), false);
        Assert.notNull(visiblePolicy);
        policyTypeIdsMap = visiblePolicy.getPolicyTypeIdsMap();
        Assert.notNull(policyTypeIdsMap);
        Assert.isTrue(policyTypeIdsMap.size() == 3);

        for (RulPolicyTypeVO policyType : policyTypes) {
            Boolean state = policyTypeIdsMap.get(policyType.getId());
            Assert.notNull(state);
            Assert.isTrue(!state);
        }

        params = new RuleController.VisiblePolicyParams();
        Map<Integer, Boolean> policyTypeIdsMapDelete = new HashMap<>();
        params.setIncludeSubtree(false);
        params.setPolicyTypeIdsMap(policyTypeIdsMapDelete);
        setVisiblePolicy(rootNode.getId(), fundVersion.getId(), params);

        visiblePolicy = getVisiblePolicy(rootNode.getId(), fundVersion.getId(), false);
        Assert.notNull(visiblePolicy);
        policyTypeIdsMap = visiblePolicy.getPolicyTypeIdsMap();
        Assert.notNull(policyTypeIdsMap);
        Assert.isTrue(policyTypeIdsMap.size() == 3);

        for (RulPolicyTypeVO policyType : policyTypes) {
            Boolean state = policyTypeIdsMap.get(policyType.getId());
            Assert.notNull(state);
            Assert.isTrue(state);
        }
    }

    private void importPackage() throws Exception {
        File file = buildPackageFileZip();
        multipart(spec -> spec.multiPart("file", file), IMPORT_PACKAGE);
        file.delete();
    }

    private void deletePackage(final String code) {
        get(spec -> spec.pathParam("code", code), DELETE_PACKAGE);
    }

    private void exportPackage(final String code) {
        get(spec -> spec.pathParam("code", code), EXPORT_PACKAGE);
    }
}
