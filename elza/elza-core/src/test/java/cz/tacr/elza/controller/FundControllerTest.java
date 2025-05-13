package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.test.controller.vo.CreateFund;
import cz.tacr.elza.test.controller.vo.FieldType;
import cz.tacr.elza.test.controller.vo.FieldValueFilter;
import cz.tacr.elza.test.controller.vo.FindFundsResult;
import cz.tacr.elza.test.controller.vo.FundsFilterField;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.FundDetail;
import cz.tacr.elza.test.controller.vo.FundSearchResult;
import cz.tacr.elza.test.controller.vo.FundsFieldName;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.NodeSearchResult;
import cz.tacr.elza.test.controller.vo.NodesFilterField;
import cz.tacr.elza.test.controller.vo.OperationCompareType;
import cz.tacr.elza.test.controller.vo.SearchParams;
import cz.tacr.elza.test.controller.vo.UpdateFund;

public class FundControllerTest extends AbstractControllerTest {

    @Test
    public void createFundTest() {
    	CreateFund cf = createFund("fund1", "fund1", 1, "aaaaaaaa-c903-4b8a-be7b-dfe15ae342e1", "mark1");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);
        assertEquals(cf.getName(), fund.getName());
        assertEquals(cf.getInternalCode(), fund.getInternalCode());
        assertEquals(cf.getUuid(), fund.getUuid());
        assertEquals(cf.getMark(), fund.getMark());
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
    public void getFundTest() {
        CreateFund cf = createFund("fund4", "fundUpd4", 4, null, "mark4");
        Fund fund = fundsApi.fundCreateFund(cf);
        assertNotNull(fund);

        FundDetail fundDetail = fundsApi.fundGetFund(fund.getId().toString());
        assertNotNull(fundDetail);
        assertEquals(fund.getId(), fundDetail.getId());
    }

    @Test
    public void searchFundsTest() {
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

    	// create filter
    	FieldValueFilter valueFilter = new FieldValueFilter();
    	valueFilter.setField(new FundsFilterField().fieldType(FieldType.FUND).fieldName(FundsFieldName.INTERNAL_CODE));
    	valueFilter.setValue("Code");
    	valueFilter.setOperation(OperationCompareType.CONTAINS);
    	params.addFiltersItem(valueFilter);

    	// fondy, jejichž interní kód obsahuje "Code" == 2
    	result = fundsApi.fundSearchFunds(params);
    	assertTrue(result.getTotalCount() == 2);

    	// change filter
    	valueFilter = new FieldValueFilter();
    	valueFilter.setField(new FundsFilterField().fieldType(FieldType.FUND).fieldName(FundsFieldName.FUND_NUMBER));
    	valueFilter.setValue("");
    	valueFilter.setOperation(OperationCompareType.NOT_NULL);
    	params.addFiltersItem(valueFilter);

    	// fondy, které mají parametr "fundNumber" (není nulový) == 1
    	result = fundsApi.fundSearchFunds(params);
    	assertTrue(result.getTotalCount() == 1);
    }

    @Test
    public void nodeSearchTest() throws InterruptedException {
    	Fund fund = createFund("fund1", "internalCode");
    	assertNotNull(fund);

    	// create ArrNode with value in fund
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_TITLE");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, "value", null, null, null);
        createDescItem(descItem, fundVersion, nodes.get(0), typeVo);

        // create MultimatchContainsFilter filter
        MultimatchContainsFilter containsFilter = new MultimatchContainsFilter();
    	containsFilter.setValue("value");

    	// create search params
    	SearchParams params = new SearchParams();
        params.setOffset(0);
        params.setSize(100);
    	params.addFiltersItem(containsFilter);

    	// waiting for reindexing to get result
    	List<FundSearchResult> fundResult = null;
    	int counter = 0;
        try {
	    	do {
	    		Thread.sleep(100);
	    		fundResult = nodeApi.nodeSearch(params);
	    		counter++;
	    	} while (fundResult.size() == 0 && counter < 1000);
	    } catch (Exception e) {
	        fail("Exception while waiting on result: " + e);
	    }
    	assertEquals(1, fundResult.size());

    	List<NodeSearchResult> nodeResult = nodeApi.nodeGetSearchResult(fund.getId());
    	assertEquals(1, nodeResult.size());
    	
        // create FieldValueFilter filter
    	FieldValueFilter valueFilter = new FieldValueFilter();
    	valueFilter.setField(new NodesFilterField().typeCode("SRD_TITLE".toLowerCase()));
    	valueFilter.setValue("alu");
    	valueFilter.setOperation(OperationCompareType.CONTAINS);
 
    	// set FieldValueFilter in params
    	params.filters(List.of(valueFilter));
    	
    	// try to search using FieldValueFilter
    	fundResult = nodeApi.nodeSearch(params);
    	assertEquals(1, fundResult.size());
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
