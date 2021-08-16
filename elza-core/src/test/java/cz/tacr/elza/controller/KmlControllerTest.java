package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 *
 */
public class KmlControllerTest extends AbstractControllerTest {

    protected final static String IMPORT_ARR_COORDINATES = KML_CONTROLLER_URL + "/import/descCoordinates";
    protected final static String IMPORT_REG_COORDINATES = KML_CONTROLLER_URL + "/import/regCoordinates";
    protected final static String EXPORT_ARR_COORDINATES = KML_CONTROLLER_URL + "/export/descCoordinates/{fundVersionId}/{descItemObjectId}";
    protected final static String EXPORT_REG_COORDINATES = KML_CONTROLLER_URL + "/export/regCoordinates/{regCoordinatesId}";

    protected final static String ALL = "coordinates/all.kml";
    protected final static String POLYGON = "coordinates/polygon.kml";
    protected final static String LINE = "coordinates/line.kml";
    protected final static String POINT = "coordinates/point.kml";

    protected final static String IMPORT_SCOPE_FA = "IMPORT_SCOPE_FA";
    protected final static String IMPORT_SCOPE_RECORD = "IMPORT_SCOPE_RECORD";
    protected final static String ALL_IN_ONE_XML = "all-in-one-import.xml";

    @After
    public void cleanUp() {
        // Extra scopes has to be deleted
        helperTestService.deleteTables(true);
        List<String> toDelete = Arrays.asList(IMPORT_SCOPE_FA, IMPORT_SCOPE_RECORD);
        for (ApScopeVO scope : getAllScopes()) {
            if (toDelete.contains(scope.getName())) {
                deleteScope(scope.getId());
                break;
            }
        }
    }

    // TODO slapa: vyřešit
    @Test
    public void arrImportExportTest() {
        List<ApScopeVO> allScopes = getAllScopes();
        importXmlFile(null, allScopes.get(0).getId(), getFile(ALL_IN_ONE_XML));

        List<ArrFundVO> funds = getFunds();
        Assert.assertEquals("Očekáváme 1 archivní pomůcku", 1, funds.size());

        Assert.assertEquals("Očekáváme jméno FA z importu", "Import z XML", funds.get(0).getName());

        int versionId = funds.get(0).getVersions().get(0).getId();

        ArrangementController.FaTreeParam faTreeParam = new ArrangementController.FaTreeParam();
        faTreeParam.setVersionId(versionId);
        TreeData faTree = getFundTree(faTreeParam);

        Assert.assertEquals("Očekáváme 3 JP + parent", 4, faTree.getNodes().size());
        ArrangementController.IdsParam idsParam = new ArrangementController.IdsParam();
        idsParam.setVersionId(versionId);
        idsParam.setIds(Collections.singletonList(faTree.getNodes().iterator().next().getId()));
        List<TreeNodeVO> nodes = getNodes(idsParam);

        TreeNodeVO treeNodeClient = nodes.get(0);
        HashMap<String, Object> params = new HashMap<>();
        params.put("fundVersionId", versionId);

        List<RulDescItemTypeExtVO> descItemTypes = getDescItemTypes();
        RulDescItemTypeExtVO cordType = null;
        for (RulDescItemTypeExtVO type : descItemTypes) {
            if (type.getCode().equals("SRD_POSITION")) {
                cordType = type;
                break;
            }
        }

        Assert.assertNotNull(cordType);

        params.put("descItemTypeId", cordType.getId());
        params.put("nodeId", treeNodeClient.getId());
        params.put("nodeVersion", treeNodeClient.getVersion());
        Integer[] ids = multipart(spec -> spec.multiPart("file", getFile(ALL)).params(params), IMPORT_ARR_COORDINATES).getBody().as(Integer[].class);
        for (Integer id : ids) {
            get(spec -> spec.pathParam("descItemObjectId", id).pathParam("fundVersionId", versionId), EXPORT_ARR_COORDINATES);
        }
        cleanUp();
    }

}
