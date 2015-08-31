package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.ParAbstractParty;
import cz.tacr.elza.domain.ParAbstractPartyVals;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;

/**
 * Testy pro {@link PartyManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
public class PartyManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String INSERT_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/insertAbstractParty";
    private static final String UPDATE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/updateAbstractParty";
    private static final String DELETE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/deleteAbstractParty";
    private static final String FIND_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/findAbstractParty";
    private static final String FIND_ABSTRACT_PARTY_COUNT =
            PARTY_MANAGER_URL + "/findAbstractPartyCount";
    private static final String GET_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/getAbstractParty";
    private static final String GET_PARTY_TYPES = PARTY_MANAGER_URL + "/getPartyTypes";

    @Test
    public void testRestInsertAbstractParty() throws Exception {

        final ParPartySubtype partySubtype = findPartySubtype();
        final RegRecord record = createRecord(1);

        ParAbstractPartyVals requestBody = new ParAbstractPartyVals();
        requestBody.setPartySubtypeId(partySubtype.getPartySubtypeId());
        requestBody.setRecordId(record.getRecordId());
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(requestBody)
                .put(INSERT_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ParAbstractParty party = response.getBody().as(ParAbstractParty.class);

        Assert.assertNotNull("Nenalezena polozka ", party);
        Assert.assertNotNull("Nenalezena polozka party subtype", party.getPartySubtype());
        Assert.assertNotNull("Nenalezena polozka record", party.getRecord());
    }

    @Test
    public void testRestUpdateAbstractParty() throws Exception {

        ParAbstractParty partyInput = createParAbstractParty();
        final RegRecord record = createRecord(2);

        ParAbstractPartyVals requestBody = new ParAbstractPartyVals();
        requestBody.setPartySubtypeId(partyInput.getPartySubtype().getPartySubtypeId());
        requestBody.setRecordId(record.getRecordId());
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("abstractPartyId", partyInput.getAbstractPartyId()).body(requestBody)
                .put(UPDATE_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ParAbstractParty party = response.getBody().as(ParAbstractParty.class);

        Assert.assertNotNull("Nenalezena polozka ", party);
        Assert.assertNotNull("Nenalezena polozka party subtype", party.getPartySubtype());
        Assert.assertNotNull("Nenalezena polozka record", party.getRecord());
        Assert.assertEquals("Nenalezena spravna polozka record", record.getRecordId(),
                party.getRecord().getRecordId());
    }

    @Test
    public void testRestDeleteAbstractParty() throws Exception {
        ParAbstractParty partyInput = createParAbstractParty();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("abstractPartyId", partyInput.getAbstractPartyId())
                .delete(DELETE_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        partyInput = abstractPartyRepository.findOne(partyInput.getAbstractPartyId());
        Assert.assertNull("Nalezena polozka ", partyInput);
    }

    @Test
    public void testRestGetAbstractParty() throws Exception {
        ParAbstractParty partyInput = createParAbstractParty();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter("abstractPartyId", partyInput.getAbstractPartyId())
                .get(GET_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        ParAbstractParty party = response.getBody().as(ParAbstractParty.class);

        Assert.assertNotNull("Nenalezena polozka ", party);
    }

    @Test
    public void testRestGetPartyTypes() throws Exception {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .get(GET_PARTY_TYPES);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        List<ParPartyType> partyTypeList = Arrays.asList(response.getBody().as(ParPartyType[].class));

        Assert.assertTrue("Nenalezena polozka ", partyTypeList.size() > 1);
    }
}
