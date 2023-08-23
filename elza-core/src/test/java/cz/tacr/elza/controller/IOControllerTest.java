package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.ExportParams;
import cz.tacr.elza.test.controller.vo.ExportRequestState;
import cz.tacr.elza.test.controller.vo.ExportRequestStatus;
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

        ExportRequestStatus expStatus = null;
        int counter = 0;
        try {
            do {
                Thread.sleep(50);
                expStatus = ioApi.ioGetExportStatus(requestId);
                counter++;
            } while (expStatus.getState() != ExportRequestState.FINISHED && counter < 1000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        assertNotNull(expStatus);
        assertEquals(ExportRequestState.FINISHED, expStatus.getState());

        File file = ioApi.ioGetExportFile(requestId);
        assertNotNull(file);
    }
}
