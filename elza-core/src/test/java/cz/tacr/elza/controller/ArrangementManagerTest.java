package cz.tacr.elza.controller;

import cz.tacr.elza.ElzaApp;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.repository.FindingAidRepository;

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
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import java.time.LocalDateTime;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Testy pro {@link ArrangementManager}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaApp.class)
@IntegrationTest("server.port:0") // zvoly volny port, lze spustit i aktivni Elzou
@WebAppConfiguration
public class ArrangementManagerTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String TEST_NAME = "TEST_X";
    private static final String TEST_UPDATE_NAME = "TEST_X2";

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
        FindingAid findingAid = arrangementManager.createFindingAid("Test name");

        arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
    }

    @Test
    public void testGetFindingAids() throws Exception {
        arrangementManager.createFindingAid("Test name");

        Assert.assertFalse(arrangementManager.getFindingAids().isEmpty());
    }

    @Test
    public void testUpdateFindingAid() throws Exception {
        FindingAid findingAid = arrangementManager.createFindingAid("Test name");

        arrangementManager.updateFindingAid(findingAid.getFindigAidId(), "Update name");
    }

    // ---- REST test ----
    @Test
    public void testRestCreateFindingAid() throws Exception {
        long pocetStart = findingAidRepository.count();

        Response response =
                given().header("content-type", "application/json").parameter("name", TEST_NAME).
                get("/api/arrangementManager/createFindingAid");

        long pocetEnd = findingAidRepository.count();
        logger.info(response.asString());
        // then
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(pocetStart + 1, pocetEnd);
    }

    @Test
    public void testRestGetFindingAid() throws Exception {
        FindingAid findingAid = new FindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(TEST_NAME);
        findingAidRepository.save(findingAid);

        Response response = given().header("content-type", "application/json")
                .get("api/arrangementManager/getFindingAids");
        logger.info(response.asString());

        Assert.assertEquals(200, response.statusCode());
        JsonPath body = response.body().jsonPath();
        List<String> nameList = body.getList("name");
        Assert.assertTrue("Nenalezena polozka " + TEST_NAME, !nameList.isEmpty());
    }

    @Test
    public void testRestDeleteFindingAid() throws Exception {
        FindingAid findingAid = new FindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(TEST_NAME);
        findingAidRepository.save(findingAid);
        Integer idFinfingAid = findingAid.getFindigAidId();
        long pocetStart = findingAidRepository.count();

        Response response = given().header("content-type", "application/json").parameter("findingAidId", idFinfingAid)
                .get("api/arrangementManager/deleteFindingAid");
        long pocetEnd = findingAidRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(pocetStart, pocetEnd + 1);
    }

    @Test
    public void testRestUpdateFindingAid() throws Exception {
        FindingAid findingAid = new FindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(TEST_NAME);
        findingAidRepository.save(findingAid);
        Integer idFinfingAid = findingAid.getFindigAidId();

        Response response = given().header("content-type", "application/json").parameter("findingAidId", idFinfingAid)
                .parameter("name", TEST_UPDATE_NAME)
                .get("api/arrangementManager/updateFindingAid");
        Assert.assertEquals(200, response.statusCode());

        boolean nalezeno = false;
        List<FindingAid> findingAids = arrangementManager.getFindingAids();
        for (FindingAid findingAidVo : findingAids) {
            if (findingAidVo.getName().equals(TEST_UPDATE_NAME)
                    && isAfterOrEqual(findingAidVo.getCreateDate(), initDate)) {
                nalezeno = true;
            }
        }
        Assert.assertTrue("Nenalezena polozka " + TEST_UPDATE_NAME, nalezeno);
      }

    private boolean isAfterOrEqual(LocalDateTime testDate, LocalDateTime initDate) {
        if (testDate == null) {
            return false;
        }
        if (initDate == null) {
            return false;
        }
        return testDate.isAfter(initDate) || testDate.isEqual(initDate);
    }
}