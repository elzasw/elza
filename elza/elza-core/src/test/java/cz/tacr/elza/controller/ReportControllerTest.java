package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.service.report.ReportService;
import cz.tacr.elza.test.controller.vo.CreateFund;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.ReportReportCategory;
import cz.tacr.elza.test.controller.vo.ReportReportData;
import cz.tacr.elza.test.controller.vo.ReportReportDefinition;
import cz.tacr.elza.test.controller.vo.ReportReportFormat;
import cz.tacr.elza.test.controller.vo.ReportReportParamValue;
import cz.tacr.elza.test.controller.vo.ReportReportParameters;
import cz.tacr.elza.test.controller.vo.ReportReportRow;
import cz.tacr.elza.test.controller.vo.ReportValue;
import cz.tacr.elza.test.controller.vo.ReportValueDate;
import cz.tacr.elza.test.controller.vo.ReportValueInteger;
import cz.tacr.elza.test.controller.vo.ReportValueType;
import cz.tacr.elza.test.controller.vo.RequestProcessState;

public class ReportControllerTest extends AbstractControllerTest {

	@Test
	public void getDefinitionsTest() {
		List<ReportReportCategory> categories = reportApi.reportGetDefinitions();
		assertTrue(!categories.isEmpty());
		// read report
		int defCount = 0;
		for (ReportReportCategory category : categories) {
			for (ReportReportDefinition definition : category.getReportDefinitions()) {
				defCount++;
			}
		}
		assertTrue(defCount>0);
	}

	@Test
	public void reportSysTotalCountTest() {
		// vytvoření alespoň jednoho fondu
		createFund("fund1", null);

        Integer requestId = reportApi.reportGenerateReport(ReportService.RT_SYS_TOTAL_COUNT, new ReportReportParameters()); 
		assertNotNull(requestId);

		RequestProcessState reportState = null;
        int counter = 0;
        try {
            do {
                Thread.sleep(100);
                reportState = reportApi.reportGetReportStatus(requestId);
                counter++;
            } while (reportState != RequestProcessState.FINISHED && counter < 1000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        assertNotNull(reportState);
        assertEquals(RequestProcessState.FINISHED, reportState);

        ReportReportData reportData = reportApi.reportGetReport(requestId, ReportReportFormat.JSON);
        List<String> headers = reportData.getHeader();
		List<ReportReportRow> rows = reportData.getRows();

        assertEquals(6, headers.size());
        assertEquals(1, rows.size());
        assertNotNull(reportData.getSourceDataDate());

		List<ReportValue> cols = rows.get(0).getCols();
		ReportValue asPocet = cols.get(0);
		ReportValue jpPocet = cols.get(1);
		ReportValue ppPocet = cols.get(2);
		ReportValue aePocet = cols.get(3);

		assertEquals(6, cols.size());
        assertEquals(ReportValueType.INT, asPocet.getValueType());
        assertEquals(ReportValueType.INT, jpPocet.getValueType());
        assertEquals(ReportValueType.INT, ppPocet.getValueType());
        assertEquals(ReportValueType.INT, aePocet.getValueType());
        assertEquals(1, ((ReportValueInteger) asPocet).getIntValue().intValue());
        assertEquals(1, ((ReportValueInteger) jpPocet).getIntValue().intValue());
        assertEquals(1, ((ReportValueInteger) ppPocet).getIntValue().intValue());
        assertEquals(3, ((ReportValueInteger) aePocet).getIntValue().intValue());
	}

	@Test
	public void reportSysMonthUserCount() {
        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        ApAccessPointVO ap = records.get(0);

        // vytvoření uživatele & login
        UsrUserVO user = createUser(ap, UserControllerTest.USER, UserControllerTest.PASS);
        UsrPermissionVO permissionVO = new UsrPermissionVO();
        permissionVO.setPermission(UsrPermission.Permission.FUND_ADMIN);
        addUserPermission(user.getId(), List.of(permissionVO));

        login(UserControllerTest.USER, UserControllerTest.PASS);

		// vytvoření fondu od vytvořeného uživatele
		createFund("fund1", user.getId());

        // vytvoření parametru
        ReportReportParamValue paramValue = new ReportReportParamValue()
        		.code("DATE")
        		.addValuesItem(new ReportValueDate().dateValue(OffsetDateTime.now()));
        ReportReportParameters reportParameters = new ReportReportParameters().addParamsItem(paramValue);

		Integer requestId = reportApi.reportGenerateReport(ReportService.RT_SYS_MONTH_USER_COUNT, reportParameters);
		assertNotNull(requestId);

		RequestProcessState reportState = null;
        int counter = 0;
        try {
            do {
                Thread.sleep(100);
                reportState = reportApi.reportGetReportStatus(requestId);
                counter++;
            } while (reportState != RequestProcessState.FINISHED && counter < 10000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        assertNotNull(reportState);
        assertEquals(RequestProcessState.FINISHED, reportState);

        ReportReportData reportData = reportApi.reportGetReport(requestId, ReportReportFormat.JSON);
        List<String> headers = reportData.getHeader();
		List<ReportReportRow> rows = reportData.getRows();

        assertEquals(14, headers.size());
        assertEquals(1, rows.size());
        assertNotNull(reportData.getSourceDataDate());

	}

	@Test
	public void reportSysInstitutionCountTest() {
		// vytvoření alespoň jednoho fondu
		createFund("fund1", null);

        // vytvoření parametru
        ReportReportParamValue paramValue = new ReportReportParamValue()
        		.code("DATE")
        		.addValuesItem(new ReportValueDate().dateValue(OffsetDateTime.now()));
        ReportReportParameters reportParameters = new ReportReportParameters().addParamsItem(paramValue);

		Integer requestId = reportApi.reportGenerateReport(ReportService.RT_SYS_INSTITUTION_COUNT, reportParameters);
		assertNotNull(requestId);

		RequestProcessState reportState = null;
        int counter = 0;
        try {
            do {
                Thread.sleep(100);
                reportState = reportApi.reportGetReportStatus(requestId);
                counter++;
            } while (reportState != RequestProcessState.FINISHED && counter < 1000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        assertNotNull(reportState);
        assertEquals(RequestProcessState.FINISHED, reportState);

        ReportReportData reportData = reportApi.reportGetReport(requestId, ReportReportFormat.JSON);
        List<String> headers = reportData.getHeader();
		List<ReportReportRow> rows = reportData.getRows();

        assertEquals(6, headers.size());
        assertEquals(1, rows.size());
        assertNotNull(reportData.getSourceDataDate());

		List<ReportValue> cols = rows.get(0).getCols();
		ReportValue fondsCnt = cols.get(2);
		ReportValue levelsCnt = cols.get(3);
		ReportValue itemsCnt = cols.get(4);

		assertEquals(6, cols.size());
		assertEquals(ReportValueType.INT, fondsCnt.getValueType());
        assertEquals(ReportValueType.INT, levelsCnt.getValueType());
        assertEquals(ReportValueType.INT, itemsCnt.getValueType());
        assertEquals(1, ((ReportValueInteger) fondsCnt).getIntValue().intValue());
        assertEquals(1, ((ReportValueInteger) levelsCnt).getIntValue().intValue());
        assertEquals(1, ((ReportValueInteger) itemsCnt).getIntValue().intValue());
	}

	protected Fund createFund(final String name, int userId) {
		List<RulRuleSetVO> ruleSets = getRuleSets();
		RulRuleSetVO ruleSet = ruleSets.get(1);
		ParInstitutionVO institution = getInstitutions().get(0);

		CreateFund createFund = new CreateFund();
		createFund.setName(name);
		createFund.setRuleSetCode(ruleSet.getCode());
		createFund.setInstitutionIdentifier(institution.getCode());
		createFund.setScopes(List.of(SCOPE_GLOBAL));
		createFund.setAdminUsers(List.of(userId));

		return fundsApi.fundCreateFund(createFund);
	}
}
