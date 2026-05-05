package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.test.controller.vo.ExportParams;
import cz.tacr.elza.test.controller.vo.ExportRequestStatus;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.FundSections;
import cz.tacr.elza.test.controller.vo.RequestProcessState;

public class IOControllerTest extends AbstractControllerTest {

    @Test
    public void ioExportFundTest() throws IOException {
        Fund fund = createFund("fundName", "internalCode");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        createLevels(fundVersion); // vytváříme úrovně

        // export otevřené verze
        ExportParams exportParams = new ExportParams();
        FundSections fundsSections = new FundSections();
        fundsSections.setFundVersionId(fundVersion.getId());
        exportParams.addFundsSectionsItem(fundsSections);

        int requestId = ioApi.ioExportRequest(exportParams);
        assertTrue(requestId > 0);
        waitForExportFinished(requestId);

        Resource openFile = ioApi.ioGetExportFile(requestId);
        assertNotNull(openFile);
        assertTrue(openFile.contentLength() > 100);

        // uzavření verze a export uzavřené verze
        helperTestService.waitForWorkers();
        approveVersion(fundVersion); // fundVersion.getId() to je již ID uzavřené verze
        helperTestService.waitForWorkers();

        requestId = ioApi.ioExportRequest(exportParams);
        assertTrue(requestId > 0);
        waitForExportFinished(requestId);

        Resource closedFile = ioApi.ioGetExportFile(requestId);
        assertNotNull(closedFile);
        assertTrue(closedFile.contentLength() > 100);
    }

    private ExportRequestStatus waitForExportFinished(int requestId) {
        ExportRequestStatus status = null;
        try {
            int counter = 0;
            do {
                Thread.sleep(50);
                status = ioApi.ioGetExportStatus(requestId);
                counter++;
            } while (status.getState() != RequestProcessState.FINISHED && counter < 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting on result: " + e);
        }
        assertNotNull(status);
        assertEquals(RequestProcessState.FINISHED, status.getState());
        return status;
    }
}
