package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class XmlImportControllerTest extends AbstractControllerTest {

    public final static String XML_FILE = "elza-import.xml";
    public final static String IMPORT = XML_IMPORT_CONTROLLER_URL + "/import";

    /**
     * Scénář
     * ----
     * - Naimportovat XML v nativním formátu ELZA (bez XSLT transformace) s jednou pomůckou, max 3 node a pár vyplněnými atributy
     * - Před importem je potřeba nahrát pravidlový balíček, xml musí odpovídat balíčku - zařízeno metodou výše
     * - Po testu ověřit, jestli se založila pomůcka a načíst strom a detail jednoho node -
     */
    @Test
    public void scenarioTest() {
        URL url = Thread.currentThread().getContextClassLoader().getResource(XML_FILE);
        File file = new File(url.getPath());
        multipart(spec -> spec.multiPart("xmlFile", file).multiPart("importDataFormat", XmlImportType.FINDING_AID).multiPart("scopeName", "TestScope"), IMPORT);
        ArrangementController.FaTreeParam faTreeParam = new ArrangementController.FaTreeParam();

        List<ArrFindingAidVO> findingAids = getFindingAids();
        Assert.assertTrue("Očekáváme 1 archivní pomůcku", findingAids.size() == 1);

        int versionId = findingAids.get(0).getVersions().get(0).getId();
        faTreeParam.setVersionId(versionId);
        TreeData faTree = getFaTree(faTreeParam);


        Assert.assertTrue("Očekáváme 3 JP + parent", faTree.getNodes().size() == 4);
        ArrangementController.IdsParam idsParam = new ArrangementController.IdsParam();
        idsParam.setVersionId(versionId);
        idsParam.setIds(Collections.singletonList(faTree.getNodes().iterator().next().getId()));
        List<TreeNodeClient> nodes = getNodes(idsParam);

        TreeNodeClient treeNodeClient = nodes.get(0);
        //treeNodeClient.get
    }

}
