package cz.tacr.elza.ws;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.ws.core.v1.CreateFundException;
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

        Assertions.assertNotNull(fundCreated);
        assertTrue(Integer.valueOf(fundCreated.getId()) >= 1);
        Assertions.assertNotNull(UUID.fromString(fundCreated.getUuid()));

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

    /**
     * A failing createFund must return the operation's declared fault (CreateFundException),
     * carrying the ErrorDescription with userMessage/detail.
     *
     * Regression test: previously FundServiceImpl flattened every failure to the generic
     * CoreServiceException (fault element errorMessage), which is not a declared fault of
     * the createFund operation. CXF could not resolve the fault detail element and failed
     * to marshal the ErrorDescription ("missing @XmlRootElement"), so the client received a
     * generic "Marshalling Error" instead of the real reason.
     */
    @Test
    public void createFundFailureReturnsTypedFault() {

        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.FUND_SERVICE_URL;
        FundService fundServiceClient = WebServiceClientFactory.createFundService(address, "admin", "admin");

        Fund fundCreate = new Fund();
        fundCreate.setFundName("Test fund with invalid institution");
        fundCreate.setRulesetCode("SIMPLE-DEV");
        // unknown institution -> server-side createFund fails
        fundCreate.setInstitutionIdentifier("non-existent-institution");
        fundCreate.setFundNumber("101");

        CreateFundException ex = Assertions.assertThrows(CreateFundException.class,
                () -> fundServiceClient.createFund(fundCreate));

        // the typed fault detail must have crossed the wire (not a marshalling error)
        Assertions.assertNotNull(ex.getFaultInfo(), "ErrorDescription fault detail must be present");
        assertTrue(ex.getFaultInfo().getUserMessage() != null
                && !ex.getFaultInfo().getUserMessage().isBlank(),
                "Fault userMessage must be populated");

        // nothing was created
        assertTrue(getFunds().isEmpty());
    }

}
