package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParDynastyVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeEditVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParUnitdateEditVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyTimeRange;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeComplementType;
import cz.tacr.elza.domain.ParPartyTypeExt;
import cz.tacr.elza.domain.ParPartyTypeRelation;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.vo.ParPartyWithCount;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
        ParUnitdateEditVO unitdateEditVO = new ParUnitdateEditVO();
        unitdateEditVO.setCalendarTypeId(calendarType.getCalendarTypeId());
        ParUnitdateEditVO unitdateEditVO2 = new ParUnitdateEditVO();
        unitdateEditVO2.setCalendarTypeId(calendarType.getCalendarTypeId());

        // names
        ParPartyNameEditVO partyNameVO = new ParPartyNameEditVO();
        partyNameVO.setMainPart("MAIN_PART" + ElzaTools.getStringOfActualDate());
        partyNameVO.setNameFormTypeId(nameFormType.getNameFormTypeId());
        partyNameVO.setPreferredName(true);
        partyNameVO.setValidFrom(unitdateEditVO);
        partyNameVO.setValidTo(unitdateEditVO2);

        // timeranges
        ParPartyTimeRangeEditVO partyTimeRangeEditVO = new ParPartyTimeRangeEditVO();
        partyTimeRangeEditVO.setFrom(unitdateEditVO);
        partyTimeRangeEditVO.setTo(unitdateEditVO2);

        // register type
        createRegisterType(TEST_CODE + ElzaTools.getStringOfActualDate(), partyType);

        ParDynastyEditVO parPartyVO = new ParDynastyEditVO();
        parPartyVO.setPartyTypeId(partyType.getPartyTypeId());
        parPartyVO.setPartyNames(Arrays.asList(partyNameVO));
        parPartyVO.addPartyTimeRange(partyTimeRangeEditVO);
        parPartyVO.setGenealogy("GENEALOGY");
        parPartyVO.setHistory("HISTORY");

        Response response = post(spec -> spec.body(parPartyVO), INSERT_PARTY_V2);
        ParPartyVO parPartyVORet = response.getBody().as(ParDynastyVO.class);

        List<ParPartyName> allNames = partyNameRepository.findAll();
        List<ParUnitdate> allUnitDate = unitdateRepository.findAll();
        List<ParPartyTimeRange> allPartyTimerange = partyTimeRangeRepository.findAll();
        List<ParParty> allParty = partyRepository.findAll();
        List<RegRecord> allRecords = recordRepository.findAll();

        Assert.assertTrue(allNames.size() == 1);
        Assert.assertTrue(allPartyTimerange.size() == 1);
        Assert.assertTrue(allRecords.size() == 1);
        Assert.assertTrue(allUnitDate.size() == 4);
        Assert.assertTrue(allParty.get(0).getHistory().equals("HISTORY"));

        // UPDATE
        parPartyVO.setPartyId(parPartyVORet.getPartyId());
        parPartyVO.getPartyNames().get(0).setPartyNameId(parPartyVORet.getPartyNames().get(0).getPartyNameId());
        parPartyVO.getTimeRanges().get(0).setPartyTimeRangeId(parPartyVORet.getTimeRanges().get(0).getPartyTimeRangeId());

        parPartyVO.setGenealogy("GENEALOGYUPDATE");
        parPartyVO.setHistory("HISTORYUPDATED");

        parPartyVO.getPartyNames().get(0).getValidTo().setUnitdateId(parPartyVORet.getPartyNames().get(0).getValidTo().getUnitdateId());
        parPartyVO.getPartyNames().get(0).setValidFrom(null);

        parPartyVO.getTimeRanges().get(0).getTo().setUnitdateId(parPartyVORet.getTimeRanges().get(0).getTo().getUnitdateId());
        parPartyVO.getTimeRanges().get(0).setFrom(null);

        response = put(spec -> spec.pathParameter(ABSTRACT_PARTY_ID_ATT, parPartyVO.getPartyId())
                                   .body(parPartyVO), UPDATE_PARTY_V2);
        parPartyVORet = response.getBody().as(ParDynastyVO.class);

        allNames = partyNameRepository.findAll(); //
        allUnitDate = unitdateRepository.findAll();//
        allPartyTimerange = partyTimeRangeRepository.findAll();
        allParty = partyRepository.findAll();
        allRecords = recordRepository.findAll();

        Assert.assertTrue(allNames.size() == 1);
        Assert.assertTrue(allPartyTimerange.size() == 1);
        Assert.assertTrue(allRecords.size() == 1);
        Assert.assertTrue(allUnitDate.size() == 2);
        Assert.assertTrue(allParty.get(0).getHistory().equals("HISTORYUPDATED"));

    }

}
