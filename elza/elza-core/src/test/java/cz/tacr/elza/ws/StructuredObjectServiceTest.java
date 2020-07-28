package cz.tacr.elza.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.StructureController.StructureDataFormDataVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.ws.core.v1.FundService;
import cz.tacr.elza.ws.core.v1.StructuredObjectService;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.ItemLong;
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
        
        ArrFundVO fundVO = new ArrFundVO();
        fundVO.setId(Integer.valueOf(fundIdents.getId()));
        ArrFundVersionVO fundVersionVO = getOpenVersion(fundVO);

        String addressSOService = RestAssured.baseURI + ":" + RestAssured.port
                + "/services"
                + WebServiceConfig.STRUCT_OBJ_SERVICE_URL;
        StructuredObjectService structObjServiceClient = DaoServiceClientFactory.createStructuredObjectService(
                                                                                                               addressSOService,
                                                                                                               "admin",
                                                                                                               "admin");

        // create using id
        StructuredObject createStructuredObject = createPacket(fundIdents, "v1");
        StructuredObjectIdentifiers sois = structObjServiceClient.createStructuredObject(createStructuredObject);
        assertNotNull(sois);
        assertTrue(StringUtils.isNotBlank(sois.getId()));

        // create using uuid
        StructuredObject createStructuredObject2 = createPacket(fundIdents, "v2");
        createStructuredObject2.setUuid(UUID.randomUUID().toString());
        StructuredObjectIdentifiers sois2 = structObjServiceClient.createStructuredObject(createStructuredObject2);
        assertNotNull(sois2);
        assertTrue(StringUtils.isNotBlank(sois2.getId()));

        // update so
        StructuredObject updateStructuredObject = createPacket(fundIdents, "v3");
        updateStructuredObject.setUuid(createStructuredObject2.getUuid());
        structObjServiceClient.updateStructuredObject(updateStructuredObject);
        // check existence of updated item
        StructureDataFormDataVO structDataVo = getFormStructureItems(fundVersionVO.getId(), Integer.valueOf(sois2
                .getId()));
        boolean itemFound = false;
        assertTrue(structDataVo.getDescItems().size()==4);
        for(ArrItemVO item: structDataVo.getDescItems()) {
            if(item instanceof ArrItemTextVO) {
                ArrItemTextVO textVo = (ArrItemTextVO)item;
                if (textVo.getValue().equals("v3")) {
                    itemFound = true;
                    break;
                }
            }
        }
        assertTrue(itemFound);

        StructuredObjectIdentifiers sois2Del = new StructuredObjectIdentifiers();
        sois2Del.setUuid(createStructuredObject2.getUuid());
        structObjServiceClient.deleteStructuredObject(sois2Del);

        structObjServiceClient.deleteStructuredObject(sois);
    }

    private StructuredObject createPacket(FundIdentifiers fundIdents, String titleValue) {
        StructuredObject createStructuredObject = new StructuredObject();
        createStructuredObject.setType("SRD_PACKET");
        createStructuredObject.setFund(fundIdents);
        createStructuredObject.setItems(createItems(titleValue));
        return createStructuredObject;
    }

    private Items createItems(String titleValue) {
        Items soItems = new Items();
        ItemString si1 = new ItemString();
        si1.setType("SRD_TITLE");
        si1.setValue(titleValue);
        soItems.getStrOrLongOrEnm().add(si1);

        ItemString si2 = new ItemString();
        si2.setType("SRD_UNIT_DATE");
        si2.setValue("2015");
        soItems.getStrOrLongOrEnm().add(si2);

        ItemLong si3 = new ItemLong();
        si3.setType("SRD_UNIT_COUNT");
        si3.setValue(5);
        soItems.getStrOrLongOrEnm().add(si3);

        ItemEnum si4 = new ItemEnum();
        si4.setType("SRD_UNIT_TYPE");
        si4.setSpec("SRD_UNIT_TYPE_LIO");
        soItems.getStrOrLongOrEnm().add(si4);
        return soItems;
    }
}
