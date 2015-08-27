package cz.tacr.elza.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegistryManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CREATE_RECORD_URL = REGISTRY_MANAGER_URL + "/createRecord";
    private static final String DELETE_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteRecord";
    private static final String FIND_RECORD_URL = REGISTRY_MANAGER_URL + "/findRecord";
    private static final String GET_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/getVariantRecords";

    private static final String RECORD_ATT = "regRecord";
    private static final String RECORD_ID_ATT = "recordId";

    private static final String SEARCH_ATT = "search";
    private static final String FROM_ATT = "from";
    private static final String COUNT_ATT = "count";
    private static final String REGISTER_TYPE_ID_ATT = "registerTypeId";


    @Autowired
    private RegistryManager registryManager;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private AbstractPartyRepository abstractPartyRepository;


    @Test
    public void testRestCreateRecord() throws Exception {
//        RegRecord record = createRecordRest();
//
//        Assert.assertNotNull(record);
//        Assert.assertNotNull(record.getRecordId());
    }

//    @Test
//    @Transactional
//    public void testUpdateRecord() throws Exception {
//
//        RegRecord regRecord2 = new RegRecord();
//        regRecord2.setRecordId(id);
//        regRecord2.setRecord("TEST RECORD 2");
//        regRecord2.setCharacteristics("TEST CHAR 2");
//        regRecord2.setLocal(true);
//
//        List<RegRegisterType> all = registerTypeRepository.findAll();
//        regRecord2.setRegisterType(all.get(0));
//
//        registryManager.updateRecord(regRecord2);
//        return;
//    }

    @Test
    public void testRestDeleteRecord() {
        RegRecord record = createRecord();

        long countStart = recordRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(RECORD_ID_ATT, record.getId())
                .get(DELETE_RECORD_URL);
        logger.info(response.asString());
        long countEnd = recordRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);
    }

    @Test
    public void testRestGetRegisterTypes() {
//        RegRecord record = createRecord();
//
//        List<RegRecord> records = registryManager.findRecord(TEST_NAME, 0, 100, record.getRegisterType().getId());
//
//        createVariantRecord("varianta", record);
//        records = registryManager.findRecord("varianta", 0, 100, record.getRegisterType().getId());
//
//        Assert.assertFalse(records.isEmpty());
    }

    private RegRecord createRecordRest() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);
        regRecord.setRegisterType(createRegisterType());

//        try {
//            new ObjectMapper().writeValue(System.out, regRecord);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        RestTemplate template = new RestTemplate();
        RequestEntity<RegRecord> requestEntity = null;
        try {
            requestEntity = RequestEntity.put(new URI("http://localhost:" + RestAssured.port + "/elza"  + CREATE_RECORD_URL)).accept(MediaType.APPLICATION_JSON).body(regRecord);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ResponseEntity<RegRecord> responseEntity = template.exchange(requestEntity, RegRecord.class);


//        Response response =
//                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(regRecord)
//                        .put(CREATE_RECORD_URL);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());

//        RegRecord record = response.getBody().as(RegRecord.class);

        RegRecord record = requestEntity.getBody();

        return record;
    }

    @Test
    public void testRestFindRecords() {
        RegRecord record = createRecord();
        createVariantRecord("varianta", record);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, TEST_NAME)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RegRecord> records = Arrays.asList(response.getBody().as(RegRecord[].class));
        Assert.assertTrue("Nenalezena polozka: " + TEST_NAME, records.size() == 1);

        createVariantRecord("varianta", record);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "varianta")
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        records = Arrays.asList(response.getBody().as(RegRecord[].class));
        Assert.assertTrue("Nenalezena polozka: " + "varianta", records.size() == 1);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_VARIANT_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        // ONDRO TADY
        List<RegVariantRecord> variantRecords = Arrays.asList(response.getBody().as(RegVariantRecord[].class));


//        List<RegRecord> records = registryManager.findRecord(TEST_NAME, 0, 100, record.getRegisterType().getId());

//        records = registryManager.findRecord("varianta", 0, 100, record.getRegisterType().getId());
    }

    protected RegRegisterType createRegisterType() {
        RegRegisterType regRegisterType = new RegRegisterType();
        regRegisterType.setCode(TEST_CODE);
        regRegisterType.setName(TEST_NAME);
        registerTypeRepository.save(regRegisterType);
        return regRegisterType;
    }

    protected RegRecord createRecord() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);
        regRecord.setRegisterType(createRegisterType());

        return recordRepository.save(regRecord);
    }

    /**
     * Vytvoří variantní záznam rejstříku
     *
     * @param obsah     textový obsah záznamu
     * @param record    záznam rejstříku ke kterému patří
     * @return          vytvořený objekt
     */
    protected RegVariantRecord createVariantRecord(final String obsah, final RegRecord record) {
        RegVariantRecord regVariantRecord = new RegVariantRecord();
        regVariantRecord.setRecord(obsah);
        regVariantRecord.setRegRecord(record);

        return variantRecordRepository.save(regVariantRecord);
    }

//    protected ParAbstractParty createAbstractParty() {
//        ParAbstractParty abstractParty = new ParAbstractParty();
//
//        return abstractPartyRepository.save(abstractParty);
//    }

}
