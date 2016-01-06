package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.config.ConfigClientVOService;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.domain.ParPartyType;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Testy pro {@link PartyManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class PartyManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigClientVOService factoryVo;

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;


//    @Test
//    public void testRestInsertParty() throws Exception {
//        ParParty party = restCreateParty();
//
//        Assert.assertNotNull("Nenalezena polozka ", party);
//        Assert.assertNotNull("Nenalezena polozka party type", party.getPartyType());
//        Assert.assertNotNull("Nenalezena polozka record", party.getRecord());
//    }
//
//    @Test
//    public void testRestUpdateParty() throws Exception {
//
//        ParParty partyInput = createParParty(1);
//        final RegRecord record = createRecord(2);
//        partyInput.setRecord(record);
//        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .body(partyInput)
//                .put(UPDATE_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(response.print(), 200, response.statusCode());
//
//        ParParty party = response.getBody().as(ParParty.class);
//
//        Assert.assertNotNull("Nenalezena polozka ", party);
//        Assert.assertNotNull("Nenalezena polozka party type", party.getPartyType());
//        Assert.assertNotNull("Nenalezena polozka record", party.getRecord());
//        Assert.assertEquals("Nenalezena spravna polozka record", record.getRecordId(),
//                party.getRecord().getRecordId());
//    }
//
//    @Test
//    public void testRestDeleteParty() throws Exception {
//        ParParty partyInput = createParParty(3);
//
//        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("partyId", partyInput.getPartyId())
//                .delete(DELETE_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//
//        partyInput = partyRepository.findOne(partyInput.getPartyId());
//        Assert.assertNull("Nalezena polozka ", partyInput);
//
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("partyId", 1875424)
//                .delete(DELETE_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(response.print(), 200, response.statusCode());
//    }
//
//    @Test
//    public void testRestGetParty() throws Exception {
//        ParParty partyInput = createParParty(4);
//
//        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("partyId", partyInput.getPartyId())
//                .get(GET_ABSTRACT_PARTY_V2);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        ParPartyVO party = response.getBody().as(ParPartyVO.class);
//
//        Assert.assertNotNull("Nenalezena polozka ", party);
//    }
//
//
//    @Test
//    public void testRestGetPartyTypes() throws Exception {
//        Response response =
//                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_PARTY_TYPES);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        List<ParPartyTypeExt> partyTypeList =
//                Arrays.asList(response.getBody().as(ParPartyTypeExt[].class));
//
//        Assert.assertTrue("Nenalezena polozka ", partyTypeList.size() > 1);
//    }
//
//    @Test
//    public void testRestFindParty() throws Exception {
//        ParParty partyInput = createParty("varianta", "KOD1");
//
//        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("search", partyInput.getRecord().getRecord())
//                .parameter("from", 0)
//                .parameter("count", 2)
//                .parameter("partyTypeId", 2)
//                .get(FIND_ABSTRACT_PARTY_V2);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        cz.tacr.elza.controller.vo.ParPartyWithCount partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//
//        Assert.assertTrue("Nenalezena polozka ", partyWithCount.getRecordList().size() == 1);
//
//        createParty("varianta", "KOD2");
//        createParty("vr 2", "KOD3");
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("search", "varianta")
//                .parameter("from", 0)
//                .parameter("count", 4)
//                .parameter("partyTypeId", 2)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//
//        Assert.assertEquals("Nenalezena polozka ", 2, partyWithCount.getRecordList().size());
//
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("search", "varianta")
//                .parameter("from", 0)
//                .parameter("count", 1)
//                .parameter("partyTypeId", 2)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//
//        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getRecordList().size());
//
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("search", "varianta")
//                .parameter("from", 1)
//                .parameter("count", 4)
//                .parameter("partyTypeId", 2)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//
//        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getRecordList().size());
//
//        // null
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("from", 0)
//                .parameter("count", 4)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//        Assert.assertEquals("Nenalezena polozka ", 3, partyWithCount.getRecordList().size());
//
//        // empty
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("search", "")
//                .parameter("from", 0)
//                .parameter("count", 4)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//        Assert.assertEquals("Nenalezena polozka ", 3, partyWithCount.getRecordList().size());
//
//        // mixed, first null, then ok
//        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("from", 0)
//                .parameter("count", 4)
//                .parameter("partyTypeId", 2)
//                .parameter("originator", true)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
//        Assert.assertEquals("Nenalezena polozka ", 3, partyWithCount.getRecordList().size());
//
//    }
//
//    @Test
//    public void testRestFindPartyCount() throws Exception {
//        ParParty partyInput = createParty("varianta", "xxx");
//        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
//                .parameter("search", partyInput.getRecord().getRecord())
//                .parameter("from", 0)
//                .parameter("count", 1)
//                .parameter("partyTypeId", 2)
//                .get(FIND_ABSTRACT_PARTY);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());
//        ParPartyWithCount partyWithCount = response.getBody().as(ParPartyWithCount.class);
//
//        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getCount().intValue());
//    }
//
//
//    @Test
//    public void testGetPartyTypes(){
//
//        ParPartyType partyType = createPartyType("p1");
//        ParRelationType relationType = createRelationType("p1");
//
//        ParPartyTypeRelation partyTypeRelation = new ParPartyTypeRelation();
//        partyTypeRelation.setPartyType(partyType);
//        partyTypeRelation.setRelationType(relationType);
//        partyTypeRelationRepository.save(partyTypeRelation);
//
//        ParRelationRoleType relationRoleType = createRelationRoleType("p1");
//        ParRelationTypeRoleType relationTypeRoleType = new ParRelationTypeRoleType();
//        relationTypeRoleType.setRelationType(relationType);
//        relationTypeRoleType.setRoleType(relationRoleType);
//        relationTypeRoleTypeRepository.save(relationTypeRoleType);
//
//
//        ParComplementType complementType = createComplementType("p1");
//        ParPartyTypeComplementType parPartyTypeComplementType = new ParPartyTypeComplementType();
//        parPartyTypeComplementType.setPartyType(partyType);
//        parPartyTypeComplementType.setComplementType(complementType);
//        partyTypeComplementTypeRepository.save(parPartyTypeComplementType);
//
//
//        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_PARTY_TYPES_V2);
//        List<LinkedHashMap<String, Object>> partyTypes = response.as(List.class);
//
//        LinkedHashMap<String, Object> p1Type = null;
//        for (LinkedHashMap type : partyTypes) {
//            if(type.get("name").equals("p1")){
//                p1Type = type;
//                break;
//            }
//        }
//        Assert.assertNotNull(p1Type);
//
//        List<LinkedHashMap> relationTypes = (List) p1Type.get("relationTypes");
//        Assert.assertNotNull(relationTypes);
//
//        List<LinkedHashMap> complementTypes = (List) p1Type.get("complementTypes");
//        Assert.assertNotNull(complementTypes);
//    }

    @Test
    public void testRestInsertPartyV2() {
        final ParPartyType partyType = findPartyType();
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParPartyTypeVO partyTypeVO = mapper.map(partyType, ParPartyTypeVO.class);

        ParPartyNameFormTypeVO partyNameFormTypeVO = new ParPartyNameFormTypeVO();
        partyNameFormTypeVO.setCode("CODE" + ElzaTools.getStringOfActualDate());

        ParPartyNameVO partyNameVO = new ParPartyNameVO();
        partyNameVO.setMainPart("MAIN_PART" + ElzaTools.getStringOfActualDate());
        partyNameVO.setNameFormType(partyNameFormTypeVO);

        RegRecordVO recordVO = createRecordVO("RECORD" + ElzaTools.getStringOfActualDate(), partyType);

        ParPartyVO parPartyVO = new ParPartyVO();
        parPartyVO.setPartyType(partyTypeVO);
        parPartyVO.setPreferredName(partyNameVO);
        parPartyVO.setRecord(recordVO);

        Response response = put(spec -> spec.body(parPartyVO), INSERT_PARTY_V2);
    }

}
