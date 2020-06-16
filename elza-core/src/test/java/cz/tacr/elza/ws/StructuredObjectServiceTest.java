package cz.tacr.elza.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.ws.core.v1.FundService;
import cz.tacr.elza.ws.core.v1.StructuredObjectService;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import cz.tacr.elza.ws.types.v1.ItemString;
import cz.tacr.elza.ws.types.v1.Items;
import cz.tacr.elza.ws.types.v1.StructuredObject;
import cz.tacr.elza.ws.types.v1.StructuredObjectIdentifiers;

public class StructuredObjectServiceTest extends AbstractControllerTest {
    @Test
    public void structObjTest() {

        String addressFundService = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.FUND_SERVICE_URL;
        FundService fundServiceClient = DaoServiceClientFactory.createFundService(addressFundService, "admin", "admin");

        Fund fundCreate = new Fund();
        fundCreate.setFundName("Test fund XYZ");
        fundCreate.setRulesetCode("SIMPLE-DEV");
        fundCreate.setInstitutionIdentifier("in1");

        FundIdentifiers fundIdents = fundServiceClient.createFund(fundCreate);

        String addressSOService = RestAssured.baseURI + ":" + RestAssured.port
                + "/services"
                + WebServiceConfig.STRUCT_OBJ_SERVICE_URL;
        StructuredObjectService structObjServiceClient = DaoServiceClientFactory.createStructuredObjectService(
                                                                                                               addressSOService,
                                                                                                               "admin",
                                                                                                               "admin");

        StructuredObject createStructuredObject = new StructuredObject();
        createStructuredObject.setType("SRD_PACKET");
        createStructuredObject.setFund(fundIdents);
        Items soItems = new Items();
        ItemString si1 = new ItemString();
        si1.setType("SRD_TITLE");
        si1.setValue("v1");
        ItemString si2 = new ItemString();
        si2.setType("SRD_UNIT_DATE");
        si2.setValue("2015");
        soItems.getStrOrLongOrEnm().add(si2);
        createStructuredObject.setItems(soItems);
        StructuredObjectIdentifiers sois = structObjServiceClient.createStructuredObject(createStructuredObject);

        assertNotNull(sois);
        assertTrue(StringUtils.isNotBlank(sois.getId()));

        structObjServiceClient.deleteStructuredObject(sois);
    }
}
