package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import cz.tacr.elza.ElzaApp;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.repository.FindingAidRepository;

/**
 * Testy pro {@link ArrangementManager}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaApp.class)
@IntegrationTest("server.port:0") // zvoli volny port, lze spustit i s aktivni Elzou
@WebAppConfiguration
public class ArrangementManagerTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String TEST_NAME = "Test name";
    private static final String TEST_UPDATE_NAME = "Update name";

    private static final String CONTENT_TYPE_HEADER = "content-type";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final String CREATE_FA_URL = "/api/arrangementManager/createFindingAid";
    private static final String UPDATE_FA_URL = "/api/arrangementManager/updateFindingAid";
    private static final String DELETE_FA_URL = "/api/arrangementManager/deleteFindingAid";
    private static final String GET_FA_URL = "/api/arrangementManager/getFindingAids";

    private static final String FA_NAME_ATT = "name";
    private static final String FA_ID_ATT = "findingAidId";

    @Value("${local.server.port}")
    private int port;
    private LocalDateTime initDate = null;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private FindingAidRepository findingAidRepository;

    @Before
    public void setUp() {
        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;
        initDate = LocalDateTime.now();
    }

    @After
    public void setDown() {
        List<FindingAid> findingAids = arrangementManager.getFindingAids();
        for (FindingAid findingAid : findingAids) {
            if ((findingAid.getName().equals(TEST_NAME) || findingAid.getName().equals(TEST_UPDATE_NAME))
                    && isAfterOrEqual(findingAid.getCreateDate(), initDate)) {
                arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
            }
        }
    }

    @Test
    public void testCreateFindingAid() throws Exception {
        arrangementManager.createFindingAid("Test name");
    }

    @Test
    public void testDeleteFindingAid() throws Exception {
        FindingAid findingAid = arrangementManager.createFindingAid(TEST_NAME);

        arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
    }

    @Test
    public void testGetFindingAids() throws Exception {
        arrangementManager.createFindingAid(TEST_NAME);

        Assert.assertFalse(arrangementManager.getFindingAids().isEmpty());
    }

    @Test
    public void testUpdateFindingAid() throws Exception {
        FindingAid findingAid = arrangementManager.createFindingAid(TEST_NAME);

        arrangementManager.updateFindingAid(findingAid.getFindigAidId(), TEST_UPDATE_NAME);
    }

    // ---- REST test ----
    @Test
    public void testRestCreateFindingAid() throws Exception {
        FindingAid findingAid = createFindingAid(TEST_NAME);

        Assert.assertNotNull(findingAid);
        Assert.assertNotNull(findingAid.getFindigAidId());
    }

    @Test
    public void testRestGetFindingAids() throws Exception {
        createFindingAid(TEST_NAME);

        List<FindingAid> findingAids = getFindingAids();
        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !findingAids.isEmpty());
    }

    @Test
    public void testRestDeleteFindingAid() throws Exception {
        Integer idFinfingAid = createFindingAid(TEST_NAME).getFindigAidId();

        long countStart = findingAidRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, idFinfingAid)
                .get(DELETE_FA_URL);
        logger.info(response.asString());
        long countEnd = findingAidRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);
    }

    @Test
    public void testRestUpdateFindingAid() throws Exception {
        Integer idFinfingAid = createFindingAid(TEST_NAME).getFindigAidId();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_ID_ATT, idFinfingAid)
                .parameter(FA_NAME_ATT, TEST_UPDATE_NAME)
                .get(UPDATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FindingAid updatedFindingAid = null;

        List<FindingAid> findingAids = getFindingAids();
        for (FindingAid findingAid : findingAids) {
            if (findingAid.getFindigAidId().equals(idFinfingAid)) {
                updatedFindingAid = findingAid;
                break;
            }
        }
        Assert.assertNotNull(updatedFindingAid);
        Assert.assertEquals(TEST_UPDATE_NAME, updatedFindingAid.getName());
    }

    /**
     * Načte archivní pomůcky přes REST volání.
     *
     * @return archivní pomůcky
     */
    private List<FindingAid> getFindingAids() {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<FindingAid> findingAids = Arrays.asList(response.getBody().as(FindingAid[].class));
        return findingAids;
    }

    /**
     * Vytvoří položku archivní pomůcky přes REST volání.
     *
     * @return vytvořená položka
     */
    private FindingAid createFindingAid(final String name) {
        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(FA_NAME_ATT, name).
                get(CREATE_FA_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        FindingAid findingAid = response.getBody().as(FindingAid.class);


        return findingAid;
    }

    private boolean isAfterOrEqual(final LocalDateTime testDate, final LocalDateTime initDate) {
        if (testDate == null) {
            return false;
        }
        if (initDate == null) {
            return false;
        }
        return testDate.isAfter(initDate) || testDate.isEqual(initDate);
    }
}