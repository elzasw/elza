package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cz.tacr.elza.controller.vo.CreateFund;
import cz.tacr.elza.controller.vo.FindFundsResult;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.FundDetail;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.UpdateFund;

public class FundControllerTest extends AbstractControllerTest {

    @Test
    public void createFund() {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulRuleSetVO ruleSet = ruleSets.get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        CreateFund cf = new CreateFund();
        cf.setInternalCode("fund1");
        cf.setName("fund1");
        cf.setInstitutionIdentifier(institution.getCode());
        cf.setRuleSetCode(ruleSet.getCode());
        cf.setUuid("aaaaaaaa-c903-4b8a-be7b-dfe15ae342e1");
        cf.setMark("mark1");
        List<String> scopes = new ArrayList<>();
        scopes.add("GLOBAL");
        cf.setScopes(scopes);
        Fund fund = createFundV1(cf);
        logger.info("Vytvořen AS : " + fund.getId());
    }

    @Test
    public void updateFund() throws ApiException {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulRuleSetVO ruleSet = ruleSets.get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        CreateFund cf = new CreateFund();
        cf.setInternalCode("fundUpd2");
        cf.setName("fund2");
        cf.setInstitutionIdentifier(institution.getCode());
        cf.setRuleSetCode(ruleSet.getCode());
        cf.setMark("mark1");
        List<String> scopes = new ArrayList<>();
        scopes.add("GLOBAL");
        cf.setScopes(scopes);
        Fund fund = createFundV1(cf);
        logger.info("Vytvořen AS : " + fund.getId());

        UpdateFund uf = new cz.tacr.elza.test.controller.vo.UpdateFund();
        uf.setInternalCode("fundUpd3");
        uf.setName("fund3");
        uf.setInstitutionIdentifier(institution.getCode());
        uf.setRuleSetCode(ruleSet.getCode());
        uf.setMark("mark3");
        uf.setFundNumber(100);
        scopes = new ArrayList<>();
        scopes.add("GLOBAL");
        uf.setScopes(scopes);
        cz.tacr.elza.test.controller.vo.FundDetail fundDetail = fundsApi.updateFund(fund.getId().toString(), uf);

        // TODO: check scope and ruleset
        // Note:  fundDetail is missing rulesetCode

        // check returned object
        assertEquals(fundDetail.getName(), uf.getName());
        assertEquals(fundDetail.getInternalCode(), uf.getInternalCode());
        assertEquals(fundDetail.getMark(), uf.getMark());
        assertEquals(fundDetail.getFundNumber(), uf.getFundNumber());
        assertEquals(fundDetail.getUnitdate(), uf.getUnitdate());

        // check DB object
        cz.tacr.elza.test.controller.vo.FundDetail fundInfo = fundsApi.getFund(fund.getId().toString());
        assertEquals(fundInfo.getName(), uf.getName());
        assertEquals(fundInfo.getInternalCode(), uf.getInternalCode());
        assertEquals(fundInfo.getMark(), uf.getMark());
        assertEquals(fundInfo.getFundNumber(), uf.getFundNumber());
        assertEquals(fundInfo.getUnitdate(), uf.getUnitdate());

        logger.info("Aktualizován AS : " + fund.getId());
    }

    @Test
    public void getFund() {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulRuleSetVO ruleSet = ruleSets.get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        CreateFund cf = new CreateFund();
        cf.setInternalCode("fundUpd4");
        cf.setName("fund4");
        cf.setInstitutionIdentifier(institution.getCode());
        cf.setRuleSetCode(ruleSet.getCode());
        cf.setMark("mark1");
        List<String> scopes = new ArrayList<>();
        scopes.add("GLOBAL");
        cf.setScopes(scopes);
        Fund fund = createFundV1(cf);
        logger.info("Vytvořen AS : " + fund.getId());

        FundDetail fundDetail = getFundV1(fund.getId());
        logger.info("Načten AS : " + fund.getId());
    }

    @Test
    public void findFunds() {

        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulRuleSetVO ruleSet = ruleSets.get(0);
        ParInstitutionVO institution = getInstitutions().get(0);

        CreateFund cf = new CreateFund();
        cf.setInternalCode("fund5");
        cf.setName("fund5");
        cf.setInstitutionIdentifier(institution.getCode());
        cf.setRuleSetCode(ruleSet.getCode());
        cf.setUuid("aaaaaaaa-1111-2222-3333-444455556666");
        cf.setMark("mark1");
        List<String> scopes = new ArrayList<>();
        scopes.add("GLOBAL");
        cf.setScopes(scopes);
        createFundV1(cf);

        cf = new CreateFund();
        cf.setInternalCode("fundUpd6");
        cf.setName("fund6");
        cf.setInstitutionIdentifier(institution.getCode());
        cf.setRuleSetCode(ruleSet.getCode());
        cf.setMark("mark1");
        scopes = new ArrayList<>();
        scopes.add("GLOBAL");
        cf.setScopes(scopes);
        createFundV1(cf);

        FindFundsResult funds = findFunds("fundUpd6", null, 10,0);
        logger.info("Nalezeno AS : " + funds.getTotalCount());
        funds = findFunds(null, "in1", 10,0);
        logger.info("Nalezeno AS: " + funds.getTotalCount());


    }

}
