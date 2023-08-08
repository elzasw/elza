package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.ExportParams;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.FundSections;

public class IOControllerTest extends AbstractControllerTest {

    @Test
    public void ioExportFundTest() throws ApiException {
        Fund fund = createFund("fundName", "internalCode");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ExportParams exportParams = new ExportParams();
        FundSections fundsSections = new FundSections();
        fundsSections.setFundVersionId(fundVersion.getId());
        exportParams.addFundsSectionsItem(fundsSections);

        int requestId = ioApi.ioExportRequest(exportParams);
        assertEquals(requestId, 1);

        while (true) {
            try {
                ioApi.ioGetExportStatus(requestId);
                break;
            } catch (ApiException ex) {
                if (ex.getCode() != 102) {
                    ex.printStackTrace();
                    break;
                }
            }
        }

        File file = ioApi.ioGetExportFile(requestId);
        assertNotNull(file);
    }
}
