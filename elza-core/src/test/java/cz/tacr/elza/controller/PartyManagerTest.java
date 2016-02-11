package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.vo.ParPartyWithCount;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Testy pro {@link PartyManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class PartyManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Test
    public void testRestInsertParty() throws Exception {
        ParParty party = restCreateParty();

        Assert.assertNotNull("Nenalezena polozka ", party);
        Assert.assertNotNull("Nenalezena polozka party type", party.getPartyType());
        Assert.assertNotNull("Nenalezena polozka record", party.getRecord());
    }

    @Test
    public void testRestUpdateParty() throws Exception {

        ParParty partyInput = createParParty(1);
        final RegRecord record = createRecord(2);
        partyInput.setRecord(record);
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .body(partyInput)
                .put(UPDATE_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(response.print(), 200, response.statusCode());

        ParParty party = response.getBody().as(ParParty.class);

        Assert.assertNotNull("Nenalezena polozka ", party);
        Assert.assertNotNull("Nenalezena polozka party type", party.getPartyType());
        Assert.assertNotNull("Nenalezena polozka record", party.getRecord());
        Assert.assertEquals("Nenalezena spravna polozka record", record.getRecordId(),
                party.getRecord().getRecordId());
    }

    @Test
    public void testRestDeleteParty() throws Exception {
        ParParty partyInput = createParParty(3);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("partyId", partyInput.getPartyId())
                .delete(DELETE_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        partyInput = partyRepository.findOne(partyInput.getPartyId());
        Assert.assertNull("Nalezena polozka ", partyInput);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("partyId", 1875424)
                .delete(DELETE_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(response.print(), 200, response.statusCode());
    }

    @Test
    public void testRestGetParty() throws Exception {
        ParParty partyInput = createParParty(4);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("partyId", partyInput.getPartyId())
                .get(GET_ABSTRACT_PARTY_V2);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ParPartyVO party = response.getBody().as(ParPartyVO.class);

        Assert.assertNotNull("Nenalezena polozka ", party);
    }


    @Test
    public void testRestGetPartyTypes() throws Exception {
        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_PARTY_TYPES);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        List<ParPartyTypeExt> partyTypeList =
                Arrays.asList(response.getBody().as(ParPartyTypeExt[].class));

        Assert.assertTrue("Nenalezena polozka ", partyTypeList.size() > 1);
    }

    @Test
    public void testRestFindParty() throws Exception {
        ParParty partyInput = createParty("varianta", "KOD1");

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", partyInput.getRecord().getRecord())
                .parameter("from", 0)
                .parameter("count", 2)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY_V2);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        cz.tacr.elza.controller.vo.ParPartyWithCount partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);

        Assert.assertTrue("Nenalezena polozka ", partyWithCount.getRecordList().size() == 1);

        createParty("varianta", "KOD2");
        createParty("vr 2", "KOD3");
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "varianta")
                .parameter("from", 0)
                .parameter("count", 4)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 2, partyWithCount.getRecordList().size());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "varianta")
                .parameter("from", 0)
                .parameter("count", 1)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getRecordList().size());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "varianta")
                .parameter("from", 1)
                .parameter("count", 4)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getRecordList().size());

        // null
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("from", 0)
                .parameter("count", 4)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
        Assert.assertEquals("Nenalezena polozka ", 3, partyWithCount.getRecordList().size());

        // empty
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "")
                .parameter("from", 0)
                .parameter("count", 4)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
        Assert.assertEquals("Nenalezena polozka ", 3, partyWithCount.getRecordList().size());

        // mixed, first null, then ok
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("from", 0)
                .parameter("count", 4)
                .parameter("partyTypeId", 2)
                .parameter("originator", true)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(cz.tacr.elza.controller.vo.ParPartyWithCount.class);
        Assert.assertEquals("Nenalezena polozka ", 3, partyWithCount.getRecordList().size());

    }

    @Test
    public void testRestFindPartyCount() throws Exception {
        ParParty partyInput = createParty("varianta", "xxx");
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", partyInput.getRecord().getRecord())
                .parameter("from", 0)
                .parameter("count", 1)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ParPartyWithCount partyWithCount = response.getBody().as(ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getCount().intValue());
    }


    @Test
    public void testGetPartyTypes(){

        ParPartyType partyType = createPartyType("p1");
        ParRelationType relationType = createRelationType("p1");

        ParPartyTypeRelation partyTypeRelation = new ParPartyTypeRelation();
        partyTypeRelation.setPartyType(partyType);
        partyTypeRelation.setRelationType(relationType);
        partyTypeRelationRepository.save(partyTypeRelation);

        ParRelationRoleType relationRoleType = createRelationRoleType("p1");
        ParRelationTypeRoleType relationTypeRoleType = new ParRelationTypeRoleType();
        relationTypeRoleType.setRelationType(relationType);
        relationTypeRoleType.setRoleType(relationRoleType);
        relationTypeRoleTypeRepository.save(relationTypeRoleType);


        ParComplementType complementType = createComplementType("p1");
        ParPartyTypeComplementType parPartyTypeComplementType = new ParPartyTypeComplementType();
        parPartyTypeComplementType.setPartyType(partyType);
        parPartyTypeComplementType.setComplementType(complementType);
        partyTypeComplementTypeRepository.save(parPartyTypeComplementType);


        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_PARTY_TYPES_V2);
        List<LinkedHashMap<String, Object>> partyTypes = response.as(List.class);

        LinkedHashMap<String, Object> p1Type = null;
        for (LinkedHashMap type : partyTypes) {
            if(type.get("name").equals("p1")){
                p1Type = type;
                break;
            }
        }
        Assert.assertNotNull(p1Type);

        List<LinkedHashMap> relationTypes = (List) p1Type.get("relationTypes");
        Assert.assertNotNull(relationTypes);

        List<LinkedHashMap> complementTypes = (List) p1Type.get("complementTypes");
        Assert.assertNotNull(complementTypes);
    }

    @Test
    public void testRestInsertUpdatePartyV2() {
        final ParPartyType partyType = findPartyType();

        ParPartyNameFormType nameFormType = createNameFormType();
        ArrCalendarType calendarType = createCalendarType();

        // unitranges
        ParUnitdateVO unitdateEditVO = new ParUnitdateVO();
        unitdateEditVO.setCalendarTypeId(calendarType.getCalendarTypeId());
        ParUnitdateVO unitdateEditVO2 = new ParUnitdateVO();
        unitdateEditVO2.setCalendarTypeId(calendarType.getCalendarTypeId());

        ParPartyNameFormTypeVO formType = new ParPartyNameFormTypeVO();
        formType.setNameFormTypeId(nameFormType.getNameFormTypeId());

        // names
        ParPartyNameVO partyNameVO = new ParPartyNameVO();
        partyNameVO.setMainPart("MAIN_PART" + ElzaTools.getStringOfActualDate());
        partyNameVO.setNameFormType(formType);
        partyNameVO.setPrefferedName(true);
        partyNameVO.setValidFrom(unitdateEditVO);
        partyNameVO.setValidTo(unitdateEditVO2);


        // register type
        RegRegisterType registerType = createRegisterType(TEST_CODE + ElzaTools.getStringOfActualDate(), partyType,
                null);

        ParPartyTypeVO partyTypeVO = new ParPartyTypeVO();
        partyTypeVO.setPartyTypeId(partyType.getPartyTypeId());

        ParDynastyVO parPartyVO = new ParDynastyVO();
        parPartyVO.setPartyType(partyTypeVO);
        parPartyVO.setPartyNames(Arrays.asList(partyNameVO));
        parPartyVO.setGenealogy("GENEALOGY");
        parPartyVO.setHistory("HISTORY");
        parPartyVO.setFrom(unitdateEditVO);
        parPartyVO.setTo(unitdateEditVO2);

        RegRecordVO record = new RegRecordVO();
        record.setRegisterTypeId(registerType.getRegisterTypeId());
        record.setScopeId(scopeRepository.findAll().get(0).getScopeId());
        parPartyVO.setRecord(record);


        Response response = post(spec -> spec.body(parPartyVO), INSERT_PARTY_V2);
        ParPartyVO parPartyVORet = response.getBody().as(ParDynastyVO.class);
        final Integer partyId = parPartyVORet.getPartyId();

        List<ParPartyName> allNames = partyNameRepository.findAll();
        List<ParUnitdate> allUnitDate = unitdateRepository.findAll();
        List<ParParty> allParty = partyRepository.findAll();
        List<RegRecord> allRecords = recordRepository.findAll();

        Assert.assertTrue(allNames.size() == 1);
        Assert.assertTrue(allRecords.size() == 1);
        Assert.assertTrue(allUnitDate.size() == 4);
        Assert.assertTrue(allParty.get(0).getHistory().equals("HISTORY"));

        Assert.assertTrue(
                recordRepository.findOne(parPartyVORet.getRecord().getRecordId()).getRecord().contains("MAIN_PART"));

        // UPDATE
        parPartyVO.setPartyId(parPartyVORet.getPartyId());
        parPartyVO.getPartyNames().get(0).setPartyNameId(parPartyVORet.getPartyNames().get(0).getPartyNameId());

        parPartyVO.setGenealogy("GENEALOGYUPDATE");
        parPartyVO.setHistory("HISTORYUPDATED");

        parPartyVO.getPartyNames().get(0).getValidTo().setUnitdateId(parPartyVORet.getPartyNames().get(0).getValidTo().getUnitdateId());
        parPartyVO.getPartyNames().get(0).setValidFrom(null);
        parPartyVO.getPartyNames().get(0).setMainPart("MAIN2");

        parPartyVO.setTo(parPartyVO.getFrom());
        parPartyVO.setFrom(null);
        parPartyVO.setVersion(parPartyVORet.getVersion());

        response = put(spec -> spec.pathParameter(ABSTRACT_PARTY_ID_ATT, parPartyVO.getPartyId())
                                   .body(parPartyVO), UPDATE_PARTY_V2);
        parPartyVORet = response.getBody().as(ParDynastyVO.class);

        allNames = partyNameRepository.findAll(); //
        allUnitDate = unitdateRepository.findAll();//
        allParty = partyRepository.findAll();
        allRecords = recordRepository.findAll();

        Assert.assertTrue(allNames.size() == 1);
        Assert.assertTrue(allRecords.size() == 1);
        Assert.assertTrue(allUnitDate.size() == 2);
        Assert.assertTrue(allParty.get(0).getHistory().equals("HISTORYUPDATED"));
        Assert.assertTrue(recordRepository.findOne(parPartyVORet.getRecord().getRecordId()).getRecord().contains("MAIN2"));


        //TEST RELATIONS

        ParRelationRoleType relationRoleType = createRelationRoleType("rrt" + ElzaTools.getStringOfActualDate());
        ParRelationRoleTypeVO relationRoleTypeVO = new ParRelationRoleTypeVO();
        relationRoleTypeVO.setRoleTypeId(relationRoleType.getRoleTypeId());

        ParRelationType relationType = createRelationType("rt" + ElzaTools.getStringOfActualDate());
        ParRelationTypeVO relationTypeVO = new ParRelationTypeVO();
        relationTypeVO.setRelationTypeId(relationType.getRelationTypeId());

        RegRecordVO recordVO = new RegRecordVO();
        recordVO.setRecordId(parPartyVORet.getRecord().getRecordId());


        ParRelationVO relationVO = new ParRelationVO();
        relationVO.setComplementType(relationTypeVO);

        ParUnitdateVO udFrom = new ParUnitdateVO();
        udFrom.setCalendarTypeId(calendarType.getCalendarTypeId());
        udFrom.setValueFrom("15.1.2015 16:00");
        udFrom.setValueFromEstimated(Boolean.FALSE);

        relationVO.setFrom(udFrom);
        relationVO.setNote("note");
        relationVO.setDateNote("datenote");
        relationVO.setPartyId(parPartyVORet.getPartyId());

        ParRelationEntityVO relationEntityVO = new ParRelationEntityVO();


        relationEntityVO.setRecord(recordVO);
        relationEntityVO.setRoleType(relationRoleTypeVO);

        relationVO.setRelationEntities(Arrays.asList(relationEntityVO));

        //INSERT RELATION
        Response relationResp = post((spec) -> spec.body(relationVO), INSERT_RELATION_V2);
        ParRelationVO relationResult = relationResp.as(ParRelationVO.class);

        ParRelation relation = relationRepository.findOne(relationResult.getRelationId());
        Assert.assertNotNull(relation);
        Assert.assertNotNull(relation.getFrom());

        List<ParRelationEntity> relationEntities = relationEntityRepository.findByRelation(relation);
        Assert.assertTrue(relationEntities.size() == 1);

        //UPDATE RELATION
        relationVO.setRelationId(relationResult.getRelationId());
        relationVO.setRelationEntities(Collections.EMPTY_LIST);
        relationVO.setNote("update");
        relationVO.setVersion(relationResult.getVersion());

        relationResp = put(
                (spec) -> spec.pathParameter(ABSTRACT_RELATION_ID_ATT, relationVO.getRelationId()).body(relationVO),
                UPDATE_RELATION_V2);
        relationResult = relationResp.as(ParRelationVO.class);
        relation = relationRepository.findOne(relationResult.getRelationId());
        Assert.assertNotNull(relation);
        Assert.assertNotNull(relation.getFrom());
        Assert.assertEquals(relation.getNote(), relationVO.getNote());


        relationEntities = relationEntityRepository.findByRelation(relation);
        Assert.assertTrue(relationEntities.isEmpty());


        //DELETE RELATION
        relationResp = delete(
                (spec) -> spec.pathParameter(ABSTRACT_RELATION_ID_ATT, relationVO.getRelationId()), DELETE_RELATION_V2);

        Assert.assertNull(relationRepository.findOne(relationVO.getRelationId()));


        //DELETE PARTY
        delete((spec)-> spec.parameter(ABSTRACT_PARTY_ID_ATT, partyId), DELETE_ABSTRACT_PARTY_V2);

        Assert.assertTrue(partyRepository.findOne(parPartyVORet.getPartyId()) == null);
    }

}
