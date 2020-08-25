package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.response.Response;

import cz.tacr.elza.controller.ArrangementController.FaTreeParam;
import cz.tacr.elza.controller.DEExportController.DEExportParamsVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.repository.StructuredItemRepository;

/**
 * Test exportu archivního souboru.
 */
public class DataExchangeControllerTest extends AbstractControllerTest {

    private final static String ALL_IN_ONE_XML = "all-in-one-import.xml";
    private final static String STRUCT_OBJ_1_TYPE = "SRD_PACKET";
    private final static String STRUCT_OBJ_1_ITEM_1_TYPE = "SRD_PACKET_TYPE";
    private final static String STRUCT_OBJ_1_ITEM_1_SPEC = "SRD_PACKET_TYPE_BOX";
    private final static int STRUCT_OBJ_1_ITEM_2_VALUE = 5238455;

    // final static String TRANSFORMATIONS = DE_IMPORT_CONTROLLER_URL + "/transformations";
    // final static String SUZAP_XML = "suzap-import.xml";

    @Autowired
    private StructuredItemRepository structItemRepository;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    public void importExportTest() throws IOException {

        // import initial data
        File file = getResourceFile(ALL_IN_ONE_XML);
        List<ApScopeVO> scopes = getAllScopes();
        ApScopeVO scope = scopes.iterator().next();
        requestImport(file, scope);

        checkData();

        // export data
        List<ArrFundVO> funds = getFunds();
        ArrFundVO fund = funds.iterator().next();
        file = downloadExport(fund);

        // clean data
        deleteFund(fund.getId());
        checkNoData();

        // import data
        requestImport(file, scope);

        checkData();
    }

    private void requestImport(final File importFile, final ApScopeVO scope) {
        importXmlFile(null, scope.getId(), importFile);
    }

    private File downloadExport(final ArrFundVO fund) throws IOException, FileNotFoundException {
        ArrFundVersionVO version = getOpenVersion(fund.getId());

        Path path = Files.createTempFile("elza-test-export", ".xml");

        FundSections fundParams = new FundSections();
        fundParams.setFundVersionId(version.getId());
        DEExportParamsVO params = new DEExportParamsVO();
        params.setFundsSections(Collections.singleton(fundParams));

        Response response = post(spec -> spec.body(params), DE_EXPORT);
        try (InputStream is = response.asInputStream()) {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        }

        return path.toFile();
    }

    private void checkData() {
        StaticDataProvider staticData = staticDataService.getData();

        // check fund exists
        List<ArrFundVO> funds = getFunds();
        Assert.assertTrue(funds.size() == 1);

        // get last fund version and rule system
        ArrFundVO fund = funds.iterator().next();
        ArrFundVersionVO fVersion = getOpenVersion(fund.getId());

        // check node count
        FaTreeParam treeParam = new FaTreeParam();
        treeParam.setVersionId(fVersion.getId());
        TreeData treeData = getFundTree(treeParam);
        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        Assert.assertTrue(nodes.size() == 4);

        // check structured object count
        FilteredResultVO<ArrStructureDataVO> structObjResult = findStructureData(STRUCT_OBJ_1_TYPE, fVersion.getId(), null, null, null, null);
        Assert.assertTrue(structObjResult.getCount() == 1);

        // check structured object item count
        Integer structObjId = structObjResult.getRows().iterator().next().getId();
        List<ArrStructuredItem> structItems = structItemRepository.findByStructuredObjectAndDeleteChangeIsNullFetchData(structObjId);
        Assert.assertTrue(structItems.size() == 2);

        // check structured object item data
        RulItemSpec so1Item1Spec = staticData.getItemTypeByCode(STRUCT_OBJ_1_ITEM_1_TYPE).getItemSpecByCode(STRUCT_OBJ_1_ITEM_1_SPEC);
        ArrDataInteger so1Item2Data = new ArrDataInteger();
        so1Item2Data.setValue(STRUCT_OBJ_1_ITEM_2_VALUE);
        so1Item2Data.setDataType(DataType.INT.getEntity());

        int foundStructData = 0;
        for (ArrStructuredItem si : structItems) {
            if (so1Item1Spec.getItemSpecId().equals(si.getItemSpecId())) {
                foundStructData++;
                continue;
            }
            ArrData data = si.getData();
            if (so1Item2Data.getDataTypeId().equals(data.getDataTypeId()) && so1Item2Data.isEqualValue(data)) {
                foundStructData++;
            }
        }
        Assert.assertTrue(foundStructData == 2);
    }

    private void checkNoData() {
        List<ArrFundVO> funds = getFunds();

        Assert.assertTrue(funds.size() == 0);
    }
}
