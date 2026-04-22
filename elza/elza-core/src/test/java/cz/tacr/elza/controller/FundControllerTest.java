package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.io.Resource;

import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.test.controller.vo.CreateFund;
import cz.tacr.elza.test.controller.vo.ExportRequestStatus;
import cz.tacr.elza.test.controller.vo.FieldType;
import cz.tacr.elza.test.controller.vo.FieldValueFilter;
import cz.tacr.elza.test.controller.vo.FindFundsResult;
import cz.tacr.elza.test.controller.vo.FondsField;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.FundDetail;
import cz.tacr.elza.test.controller.vo.FondsFieldName;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.OperationCompareType;
import cz.tacr.elza.test.controller.vo.RequestProcessState;
import cz.tacr.elza.test.controller.vo.SearchParams;
import cz.tacr.elza.test.controller.vo.UpdateFund;

/**
 * Tests for fund CRUD and search operations.
 *
 * Uses per-class lifecycle: setUp/tearDown run once for all test methods.
 * Each test cleans up funds it creates to keep the database in a consistent state.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FundControllerTest extends AbstractControllerTest {

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    @Test
    public void createFundTest() {
    	CreateFund cf = createFund("fund1", "fund1", 1, "aaaaaaaa-c903-4b8a-be7b-dfe15ae342e1", "mark1");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);
        assertEquals(cf.getName(), fund.getName());
        assertEquals(cf.getInternalCode(), fund.getInternalCode());
        assertEquals(cf.getUuid(), fund.getUuid());
        assertEquals(cf.getMark(), fund.getMark());

        deleteFund(fund.getId());
    }

    @Test
    public void updateFundTest() {
    	CreateFund cf = createFund("fund2", "fundUpd2", 2, null, "mark1");
        Fund fund = fundsApi.fundCreateFund(cf);

        UpdateFund uf = createUpdateFund("fund3", "fundUpd3", 100, "mark3");
        FundDetail fundDetail = fundsApi.fundUpdateFund(fund.getId().toString(), uf);

        // TODO: check scope and ruleset
        // Note:  fundDetail is missing rulesetCode

        // check returned object
        assertEquals(uf.getName(), fundDetail.getName());
        assertEquals(uf.getInternalCode(), fundDetail.getInternalCode());
        assertEquals(uf.getMark(), fundDetail.getMark());
        assertEquals(uf.getFundNumber(), fundDetail.getFundNumber());
        assertEquals(uf.getUnitdate(), fundDetail.getUnitdate());

        // check DB object
        FundDetail fundInfo = fundsApi.fundGetFund(fund.getId().toString());
        assertEquals(uf.getName(), fundInfo.getName());
        assertEquals(uf.getInternalCode(), fundInfo.getInternalCode());
        assertEquals(uf.getMark(), fundInfo.getMark());
        assertEquals(uf.getFundNumber(), fundInfo.getFundNumber());
        assertEquals(uf.getUnitdate(), fundInfo.getUnitdate());

        deleteFund(fund.getId());
    }

    @Test
    public void getFundTest() {
        CreateFund cf = createFund("fund4", "fundUpd4", 4, null, "mark4");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);

        FundDetail fundDetail = fundsApi.fundGetFund(fund.getId().toString());
        assertNotNull(fundDetail);
        assertEquals(fund.getId(), fundDetail.getId());

        deleteFund(fund.getId());
    }

    @Test
    public void searchFundsTest() {
        CreateFund cf = createFund("fund1", "fundCode1", 1, "aaaaaaaa-1111-2222-3333-444455556666", "mark1");
        Fund fund1 = fundsApi.fundCreateFund(cf);

        cf = createFund("fund2", "fundCode2", null, null, "mark2");
        Fund fund2 = fundsApi.fundCreateFund(cf);

        cf = createFund("fund3", "fund3", null, null, "mark3");
        Fund fund3 = fundsApi.fundCreateFund(cf);

        try {
            SearchParams params = new SearchParams();
            params.setOffset(0);
            params.setSize(100);

            FindFundsResult result = fundsApi.fundSearchFunds(params);
            assertEquals(3, result.getTotalCount());

            MultimatchContainsFilter containsFilter = new MultimatchContainsFilter();
            containsFilter.setValue("fund");
            params.addFiltersItem(containsFilter);

            // fondy s fragmentem "fund" v názvu == 3
            result = fundsApi.fundSearchFunds(params);
            assertEquals(3, result.getTotalCount());

            // create filter
            FieldValueFilter valueFilter = new FieldValueFilter();
            valueFilter.setField(new FondsField().fieldType(FieldType.FONDS_FIELD).fieldName(FondsFieldName.INTERNAL_CODE));
            valueFilter.setValue("Code");
            valueFilter.setOperation(OperationCompareType.CONTAINS);
            params.addFiltersItem(valueFilter);

            // fondy, jejichž interní kód obsahuje "Code" == 2
            result = fundsApi.fundSearchFunds(params);
            assertEquals(2, result.getTotalCount());

            // change filter
            valueFilter = new FieldValueFilter();
            valueFilter.setField(new FondsField().fieldType(FieldType.FONDS_FIELD).fieldName(FondsFieldName.FONDS_NUMBER));
            valueFilter.setValue("");
            valueFilter.setOperation(OperationCompareType.NOT_NULL);
            params.addFiltersItem(valueFilter);

            // fondy, které mají parametr "fundNumber" (není nulový) == 1
            result = fundsApi.fundSearchFunds(params);
            assertEquals(1, result.getTotalCount());
        } finally {
            deleteFund(fund1.getId());
            deleteFund(fund2.getId());
            deleteFund(fund3.getId());
        }
    }

    @Test
    public void fundExportFunds() throws IOException, InterruptedException {
        CreateFund cf = createFund("fundExport", "internalCode", 1, "aaaaaaaa-1111-2222-3333-444455556666", "mark1");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);

        try {
            MultimatchContainsFilter containsFilter = new MultimatchContainsFilter();
            containsFilter.setValue("fund");

            SearchParams params = new SearchParams().addFiltersItem(containsFilter);

            int requestId = fundsApi.fundExportFunds(params);
            assertTrue(requestId > 0);

            // wait for export to finish (max 10 seconds)
            ExportRequestStatus expStatus = null;
            for (int i = 0; i < 200; i++) {
                Thread.sleep(50);
                expStatus = ioApi.ioGetExportStatus(requestId);
                if (expStatus.getState() == RequestProcessState.FINISHED) {
                    break;
                }
            }
            assertNotNull(expStatus);
            assertEquals(RequestProcessState.FINISHED, expStatus.getState());

            Resource file = ioApi.ioGetExportFile(requestId);
            assertNotNull(file);
            assertTrue(file.contentLength() > 100);
        } finally {
            deleteFund(fund.getId());
        }
    }
    
    private CreateFund createFund(String name, String internalCode, Integer fundNumber, String uuid, String mark) {
        RulRuleSetVO ruleSet = getRuleSets().get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        CreateFund cf = new CreateFund();
        cf.setName(name);
        cf.setInternalCode(internalCode);
        cf.setInstitutionIdentifier(institution.getCode());
        cf.setRuleSetCode(ruleSet.getCode());
        cf.setFundNumber(fundNumber);
        cf.setUuid(uuid);
        cf.setMark(mark);
        cf.setScopes(Arrays.asList(SCOPE_GLOBAL));

        return cf;
    }

    private UpdateFund createUpdateFund(String name, String internalCode, Integer fundNumber, String mark) {
        RulRuleSetVO ruleSet = getRuleSets().get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        UpdateFund uf = new cz.tacr.elza.test.controller.vo.UpdateFund();
        uf.setName(name);
        uf.setInternalCode(internalCode);
        uf.setInstitutionIdentifier(institution.getCode());
        uf.setRuleSetCode(ruleSet.getCode());
        uf.setFundNumber(fundNumber);
        uf.setMark(mark);
        uf.setScopes(Arrays.asList(SCOPE_GLOBAL));

        return uf;
    }
}
