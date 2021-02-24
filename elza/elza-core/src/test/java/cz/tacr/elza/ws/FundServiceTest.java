package cz.tacr.elza.ws;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.ws.core.v1.FundService;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import io.restassured.RestAssured;

public class FundServiceTest extends AbstractControllerTest {

    @Test
    public void fundTest() {

        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.FUND_SERVICE_URL;
        FundService fundServiceClient = WebServiceClientFactory.createFundService(address, "admin", "admin");
        
        Fund fundCreate = new Fund();
        fundCreate.setFundName("Test fund XYZ");
        fundCreate.setRulesetCode("SIMPLE-DEV");
        fundCreate.setInstitutionIdentifier("in1");
        fundCreate.setDateRange("date range");
        fundCreate.setInternalCode("internal code");
        fundCreate.setFundNumber("100");

        FundIdentifiers fundCreated = fundServiceClient.createFund(fundCreate);

        Assert.assertNotNull(fundCreated);
        assertTrue(Integer.valueOf(fundCreated.getId()) >= 1);
        Assert.assertNotNull(UUID.fromString(fundCreated.getUuid()));

        List<ArrFundVO> funds = getFunds();
        assertTrue(funds.size() == 1);
        ArrFundVO fundVO = funds.get(0);
        assertTrue(fundVO.getName().equals(fundCreate.getFundName()));
        assertEquals(fundVO.getFundNumber(), 100);
        assertEquals(fundVO.getInternalCode(), fundCreate.getInternalCode());
        assertEquals(fundVO.getMark(), fundCreate.getMark());
        assertEquals(fundVO.getUnitdate(), fundCreate.getDateRange());

        fundServiceClient.deleteFund(fundCreated);

        funds = getFunds();
        assertTrue(funds.size() == 0);
    }

}
