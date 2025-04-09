package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.test.controller.vo.CreateFund;
import cz.tacr.elza.test.controller.vo.FieldValueFilter;
import cz.tacr.elza.test.controller.vo.FindFundsResult;
import cz.tacr.elza.test.controller.vo.FondsFilterField;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.FundDetail;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.OperationCompareType;
import cz.tacr.elza.test.controller.vo.SearchParams;
import cz.tacr.elza.test.controller.vo.UpdateFund;

public class FundControllerTest extends AbstractControllerTest {

    @Test
    public void createFund() {
    	CreateFund cf = createFund("fund1", "fund1", 1, "aaaaaaaa-c903-4b8a-be7b-dfe15ae342e1", "mark1");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);
        assertEquals(cf.getName(), fund.getName());
        assertEquals(cf.getInternalCode(), fund.getInternalCode());
        assertEquals(cf.getUuid(), fund.getUuid());
        assertEquals(cf.getMark(), fund.getMark());
    }

    @Test
    public void updateFund() {
    	CreateFund cf = createFund("fund2", "fundUpd2", 2, null, "mark1");
        Fund fund = fundsApi.fundCreateFund(cf);

        UpdateFund uf = createUpdateFund("fund3", "fundUpd3", 100, "mark3");
        FundDetail fundDetail = fundsApi.fundUpdateFund(fund.getId().toString(), uf);

        // TODO: check scope and ruleset
        // Note:  fundDetail is missing rulesetCode

        // check returned object
        assertEquals(fundDetail.getName(), uf.getName());
        assertEquals(fundDetail.getInternalCode(), uf.getInternalCode());
        assertEquals(fundDetail.getMark(), uf.getMark());
        assertEquals(fundDetail.getFundNumber(), uf.getFundNumber());
        assertEquals(fundDetail.getUnitdate(), uf.getUnitdate());

        // check DB object
        FundDetail fundInfo = fundsApi.fundGetFund(fund.getId().toString());
        assertEquals(fundInfo.getName(), uf.getName());
        assertEquals(fundInfo.getInternalCode(), uf.getInternalCode());
        assertEquals(fundInfo.getMark(), uf.getMark());
        assertEquals(fundInfo.getFundNumber(), uf.getFundNumber());
        assertEquals(fundInfo.getUnitdate(), uf.getUnitdate());
    }

    @Test
    public void getFund() {
        CreateFund cf = createFund("fund4", "fundUpd4", 4, null, "mark4");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);

        FundDetail fundDetail = fundsApi.fundGetFund(fund.getId().toString());
        assertNotNull(fundDetail);
        assertEquals(fund.getId(), fundDetail.getId());
    }

    @Test
    public void findFunds() {
        CreateFund cf = createFund("fund5", "fund5", 5, "aaaaaaaa-1111-2222-3333-444455556666", "mark5");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);

        cf = createFund("fund6", "fundUpd6", 6, null, "mark6");
        fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);

        FindFundsResult funds = findFunds("fundUpd6", null, 10, 0);
        assertTrue(funds.getTotalCount() == 1);

        funds = findFunds(null, "in1", 10, 0);
        assertTrue(funds.getTotalCount() == 2);
    }

    @Test
    public void searchFunds() {
        CreateFund cf = createFund("fund1", "fundCode1", 1, "aaaaaaaa-1111-2222-3333-444455556666", "mark1");
        fundsApi.fundCreateFund(cf);

        cf = createFund("fund2", "fundCode2", null, null, "mark2");
        fundsApi.fundCreateFund(cf);

        cf = createFund("fund3", "fund3", null, null, "mark3");
        fundsApi.fundCreateFund(cf);

        SearchParams params = new SearchParams();
        params.setOffset(0);
        params.setSize(100);

    	FindFundsResult result = fundsApi.fundSearchFunds(params);
    	assertTrue(result.getTotalCount() == 3);

    	MultimatchContainsFilter containsFilter = new MultimatchContainsFilter();
    	containsFilter.setValue("fund");
    	params.addFiltersItem(containsFilter);

    	// fondy s fragmentem "fund" v názvu == 3
    	result = fundsApi.fundSearchFunds(params);
    	assertTrue(result.getTotalCount() == 3);

    	FieldValueFilter valueFilter = new FieldValueFilter();
    	valueFilter.setField(FondsFilterField.INTERNAL_CODE);
    	valueFilter.setValue("Code");
    	valueFilter.setOperation(OperationCompareType.CONTAINS);
    	params.addFiltersItem(valueFilter);

    	// fondy, jejichž interní kód obsahuje "Code" == 2
    	result = fundsApi.fundSearchFunds(params);
    	assertTrue(result.getTotalCount() == 2);

    	valueFilter = new FieldValueFilter();
    	valueFilter.setField(FondsFilterField.FUND_NUMBER);
    	valueFilter.setValue("");
    	valueFilter.setOperation(OperationCompareType.NOT_NULL);
    	params.addFiltersItem(valueFilter);

    	// fondy, které mají parametr "fundNumber" (není nulový) == 1
    	result = fundsApi.fundSearchFunds(params);
    	assertTrue(result.getTotalCount() == 1);
    }

    private CreateFund createFund(String name, String internalCode, Integer fundNumber, String uuid, String mark) {
        RulRuleSetVO ruleSet = getRuleSets().get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        CreateFund cf = new CreateFund();
        cf = new CreateFund();
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
