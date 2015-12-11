package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyTypeExt;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.vo.ParPartyWithCount;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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

        ParParty partyInput = createParParty();
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
        ParParty partyInput = createParParty();

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
        ParParty partyInput = createParParty();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("partyId", partyInput.getPartyId())
                .get(GET_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ParParty party = response.getBody().as(ParParty.class);

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
        ParParty partyInput = createParty("varianta");

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", partyInput.getRecord().getRecord())
                .parameter("from", 0)
                .parameter("count", 2)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ParPartyWithCount partyWithCount = response.getBody().as(ParPartyWithCount.class);

        Assert.assertTrue("Nenalezena polozka ", partyWithCount.getPartyList().size() == 1);

        partyInput = createParty("varianta");
        partyInput = createParty("vr 2");
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "varianta")
                .parameter("from", 0)
                .parameter("count", 4)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 2, partyWithCount.getPartyList().size());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "varianta")
                .parameter("from", 0)
                .parameter("count", 1)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getPartyList().size());

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("search", "varianta")
                .parameter("from", 1)
                .parameter("count", 4)
                .parameter("partyTypeId", 2)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        partyWithCount = response.getBody().as(ParPartyWithCount.class);

        Assert.assertEquals("Nenalezena polozka ", 1, partyWithCount.getPartyList().size());
    }

    @Test
    public void testRestFindPartyCount() throws Exception {
        ParParty partyInput = createParty("varianta");
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
}
