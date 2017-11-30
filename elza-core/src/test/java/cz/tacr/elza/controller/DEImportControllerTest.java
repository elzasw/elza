package cz.tacr.elza.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;

/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class DEImportControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(DEImportControllerTest.class);

    protected final static String TRANSFORMATIONS = DE_IMPORT_CONTROLLER_URL + "/transformations";

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
        List<RegScopeVO> allScopes = getAllScopes();
        importXmlFile(null, allScopes.get(0).getId(), getResourceFile(ALL_IN_ONE_XML));

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

        /*
        TreeNodeClient treeNodeClient = nodes.get(0);
        importXmlFile(null, 1, getResourceFile(ALL_IN_ONE_XML));
        importXmlFile(null, 1, getResourceFile(ALL_IN_ONE_XML));
        */
    }

    public static File getResourceFile(String resourcePath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        Assert.assertNotNull(url);
        return new File(url.getPath());
    }

    @Test
    @Ignore
    public void importWithTransformation() throws IOException {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        Assert.assertTrue(!ruleSets.isEmpty());
        File source = getResourceFile(SUZAP_XSLT);

        File dir = new File(this.transformationsDirectory);
        logger.info(this.transformationsDirectory);
        logger.info(dir.getAbsolutePath());

        if (!dir.exists()) {
            Assert.assertTrue(dir.mkdirs());
        }

        File dest = new File(this.transformationsDirectory + File.separator + "suzap.xslt");
        FileCopyUtils.copy(source, dest);
        Integer ruleSetId = ruleSets.iterator().next().getId();

        try {
            importXmlFile(INVALID_TRANSFORMATION_NAME, 1, getResourceFile(SUZAP_XML));
        } catch (AssertionError e) {
            /** Test chybné transformace */
        }

        try {
            importXmlFile(TRANSFORMATION_NAME, 1, getResourceFile(SUZAP_XML));
        } catch (AssertionError e) {
            /** Ochrana proti chybné XSLT transformaci */
        }
    }

    @Test
    public void getTransformationsTest() {
        get(TRANSFORMATIONS);
    }
}
