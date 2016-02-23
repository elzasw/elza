package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class XmlImportControllerTest extends AbstractControllerTest {

    public final static String XML_FILE = "elza-import.xml";
    public final static String IMPORT = XML_IMPORT_CONTROLLER_URL + "/import";

    public final static String IMPORT_SCOPE = "ImportScope";

    /**
     * Scénář
     * ----
     * - Naimportovat XML v nativním formátu ELZA (bez XSLT transformace) s jednou pomůckou, max 3 node a pár vyplněnými atributy
     * - Před importem je potřeba nahrát pravidlový balíček, xml musí odpovídat balíčku - zařízeno metodou výše
     * - Po testu ověřit, jestli se založila pomůcka a načíst strom a detail jednoho node -
     */
    @Test
    public void scenarioTest() {
        for (RegScopeVO scope : getAllScopes()) {
            if (scope.getName().equals(XmlImportControllerTest.IMPORT_SCOPE)) {
                deleteScope(scope.getId());
                break;
            }
        }
        importFA();

        List<ArrFindingAidVO> findingAids = getFindingAids();
        Assert.assertTrue("Očekáváme 1 archivní pomůcku", findingAids.size() == 1);

        Assert.assertTrue("Očekáváme jméno FA z importu", findingAids.get(0).getName().equals("Import z XML"));

        int versionId = findingAids.get(0).getVersions().get(0).getId();

        ArrangementController.FaTreeParam faTreeParam = new ArrangementController.FaTreeParam();
        faTreeParam.setVersionId(versionId);
        TreeData faTree = getFaTree(faTreeParam);

        Assert.assertTrue("Očekáváme 3 JP + parent", faTree.getNodes().size() == 4);
        ArrangementController.IdsParam idsParam = new ArrangementController.IdsParam();
        idsParam.setVersionId(versionId);
        idsParam.setIds(Collections.singletonList(faTree.getNodes().iterator().next().getId()));
        List<TreeNodeClient> nodes = getNodes(idsParam);

        TreeNodeClient treeNodeClient = nodes.get(0);
    }

    public static void importFA() {
        URL url = Thread.currentThread().getContextClassLoader().getResource(XML_FILE);
        File file = new File(url.getPath());
        multipart(spec -> spec.multiPart("xmlFile", file).multiPart("importDataFormat", XmlImportType.FINDING_AID).multiPart("scopeName", IMPORT_SCOPE), IMPORT);
    }
}
