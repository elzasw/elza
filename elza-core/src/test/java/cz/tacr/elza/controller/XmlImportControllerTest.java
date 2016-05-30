package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;


/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class XmlImportControllerTest extends AbstractControllerTest {

    protected final static String TRANSFORMATIONS = XML_IMPORT_CONTROLLER_URL + "/transformations";

    protected final static String IMPORT_SCOPE_FA = "IMPORT_SCOPE_FA";
    protected final static String IMPORT_SCOPE_PARTY = "IMPORT_SCOPE_PARTY";
    protected final static String IMPORT_SCOPE_RECORD = "IMPORT_SCOPE_RECORD";
    protected final static String ALL_IN_ONE_XML = "all-in-one-import.xml";
    protected final static String SUZAP_XML = "suzap-import.xml";
    protected final static String SUZAP_XSLT = "zp/imports/suzap.xslt";
    protected final static String TRANSFORMATION_NAME = "suzap";
    protected final static String INVALID_TRANSFORMATION_NAME = "invalid unknown";

    @Value("${elza.xmlImport.transformationDir}")
    private String transformationsDirectory;

    @After
    public void cleanUp() {
        List<String> toDelete = Arrays.asList(IMPORT_SCOPE_FA, IMPORT_SCOPE_PARTY, IMPORT_SCOPE_RECORD);
        for (RegScopeVO scope : getAllScopes()) {
            if (toDelete.contains(scope.getName())) {
                deleteScope(scope.getId());
                break;
            }
        }
    }

    /**
     * Scénář
     * ----
     * - Naimportovat XML v nativním formátu ELZA (bez XSLT transformace) s jednou pomůckou, max 3 node a pár vyplněnými atributy
     * - Před importem je potřeba nahrát pravidlový balíček, xml musí odpovídat balíčku - zařízeno metodou výše
     * - Po testu ověřit, jestli se založila pomůcka a načíst strom a detail jednoho node -
     * ----
     * Import osob
     * Import rejstříků
     *
     */
    @Test
    public void scenarioTest() {
        importFile(getFile(ALL_IN_ONE_XML), IMPORT_SCOPE_FA, XmlImportType.FUND, null, null, null);
        List<RegScopeVO> allScopes = getAllScopes();
        importFile(getFile(ALL_IN_ONE_XML), null, XmlImportType.FUND, allScopes.get(0).getId(), null, null);

        List<ArrFundVO> funds = getFunds();
        Assert.assertTrue("Očekáváme 1 archivní pomůcku", funds.size() == 1);

        Assert.assertTrue("Očekáváme jméno FA z importu", funds.get(0).getName().equals("Import z XML"));

        int versionId = funds.get(0).getVersions().get(0).getId();

        ArrangementController.FaTreeParam faTreeParam = new ArrangementController.FaTreeParam();
        faTreeParam.setVersionId(versionId);
        TreeData faTree = getFundTree(faTreeParam);

        Assert.assertTrue("Očekáváme 3 JP + parent", faTree.getNodes().size() == 4);
        ArrangementController.IdsParam idsParam = new ArrangementController.IdsParam();
        idsParam.setVersionId(versionId);
        idsParam.setIds(Collections.singletonList(faTree.getNodes().iterator().next().getId()));
        List<TreeNodeClient> nodes = getNodes(idsParam);

        TreeNodeClient treeNodeClient = nodes.get(0);
        importFile(getFile(ALL_IN_ONE_XML), IMPORT_SCOPE_RECORD, XmlImportType.RECORD, null, null, null);
        importFile(getFile(ALL_IN_ONE_XML), IMPORT_SCOPE_PARTY, XmlImportType.PARTY, null, null, null);

    }

    private void importFile(File xmlFile, String scopeName, XmlImportType type, Integer scopeId, String transformation, Integer ruleSetId) {
        importXmlFile(transformation, null, type, scopeName, scopeId, xmlFile, ruleSetId);
    }

    public static File getFile(String resourcePath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        Assert.assertNotNull(url);
        return new File(url.getPath());
    }

    @Test
    public void importWithTransformation() throws IOException {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        Assert.assertTrue(!ruleSets.isEmpty());
        File source = getFile(SUZAP_XSLT);
        File dest = new File(this.transformationsDirectory + "/suzap.xslt");
        FileCopyUtils.copy(source, dest);
        Integer ruleSetId = ruleSets.iterator().next().getId();

        try {
            importFile(getFile(SUZAP_XML), INVALID_TRANSFORMATION_NAME, XmlImportType.FUND, null, TRANSFORMATION_NAME, ruleSetId);
        } catch (AssertionError e) {
            /** Test chybné transformace */
        }

        try {
            importFile(getFile(SUZAP_XML), TRANSFORMATION_NAME, XmlImportType.FUND, null, TRANSFORMATION_NAME, ruleSetId);
        } catch (AssertionError e) {
            /** Ochrana proti chybné XSLT transformaci */
        }
    }

    @Test
    public void getTransformationsTest() {
        get(TRANSFORMATIONS);
    }
}
