package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.domain.RulDescItemType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URL;
import java.util.*;


/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class KmlControllerTest extends AbstractControllerTest {

    protected final static String IMPORT_ARR_COORDINATES = KML_CONTROLLER_URL + "/import/arrCoordinates";
    protected final static String IMPORT_REG_COORDINATES = KML_CONTROLLER_URL + "/import/regCoordinates";
    protected final static String EXPORT_ARR_COORDINATES = KML_CONTROLLER_URL + "/export/arrCoordinates/{descItemObjectId}/{fundVersionId}";
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
        deleteTables();
        List<String> toDelete = Arrays.asList(IMPORT_SCOPE_FA, IMPORT_SCOPE_RECORD);
        for (RegScopeVO scope : getAllScopes()) {
            if (toDelete.contains(scope.getName())) {
                deleteScope(scope.getId());
                break;
            }
        }
    }

    @Test
    public void arrImportExportTest() {
        importFile(getFile(ALL_IN_ONE_XML), IMPORT_SCOPE_FA, XmlImportType.FUND, null);
        List<RegScopeVO> allScopes = getAllScopes();
        importFile(getFile(ALL_IN_ONE_XML), null, XmlImportType.FUND, allScopes.get(0).getId());

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
        HashMap<String, Object> params = new HashMap<>();
        params.put("fundVersionId", versionId);

        List<RulDescItemTypeExtVO> descItemTypes = getDescItemTypes();
        RulDescItemTypeExtVO cordType = null;
        for (RulDescItemTypeExtVO type : descItemTypes) {
            if (type.getCode().equals("ZP2015_POSITION")) {
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

    @Test
    public void regImportExportTest() {
        importFile(getFile(ALL_IN_ONE_XML), IMPORT_SCOPE_RECORD, XmlImportType.RECORD, null);
        List<RegScopeVO> allScopes = getAllScopes();
        importFile(getFile(ALL_IN_ONE_XML), null, XmlImportType.RECORD, allScopes.get(0).getId());


        List<RegRecordVO> records = findRecord(null, 0, 10, null, null, null);
        Assert.assertTrue(!records.isEmpty());
        RegRecordVO record = records.iterator().next();

        HashMap<String, Object> params = new HashMap<>();
        params.put("regRecordId", record.getRecordId());
        Integer[] ids = multipart(spec -> spec.multiPart("file", getFile(ALL)).params(params), IMPORT_REG_COORDINATES).getBody().as(Integer[].class);
        for (Integer id : ids) {
            get(spec -> spec.pathParam("regCoordinatesId", id), EXPORT_REG_COORDINATES);
        }
    }

    private void importFile(File xmlFile, String scopeName, XmlImportType type, @Nullable Integer scopeId) {
        importXmlFile(null, null, type, scopeName, scopeId, xmlFile);
    }

    public static File getFile(String resourcePath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        Assert.assertNotNull(url);
        return new File(url.getPath());
    }
}
