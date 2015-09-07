package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Test pro operace s rejstříkem.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegistryManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CREATE_RECORD_URL = REGISTRY_MANAGER_URL + "/createRecord";
    private static final String UPDATE_RECORD_URL = REGISTRY_MANAGER_URL + "/updateRecord";
    private static final String DELETE_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteRecord";

    private static final String CREATE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/createVariantRecord";
    private static final String UPDATE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/updateVariantRecord";
    private static final String DELETE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteVariantRecord";

    private static final String FIND_RECORD_URL = REGISTRY_MANAGER_URL + "/findRecord";
    private static final String FIND_RECORD_COUNT_URL = REGISTRY_MANAGER_URL + "/findRecordCount";

    private static final String GET_REGISTER_TYPES_URL = REGISTRY_MANAGER_URL + "/getRegisterTypes";
    private static final String GET_EXTERNAL_SOURCES_URL = REGISTRY_MANAGER_URL + "/getExternalSources";
    private static final String GET_RECORD_URL = REGISTRY_MANAGER_URL + "/getRecord";

    private static final String RECORD_ID_ATT = "recordId";
    private static final String VARIANT_RECORD_ID_ATT = "variantRecordId";

    private static final String SEARCH_ATT = "search";
    private static final String FROM_ATT = "from";
    private static final String COUNT_ATT = "count";
    private static final String REGISTER_TYPE_ID_ATT = "registerTypeId";
    private static final String EXTERNAL_SOURCE_ID_ATT = "externalSourceId";


    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private ExternalSourceRepository externalSourceRepository;


    /**
     * Vytvoření záznamu rejstříku.
     */
    @Test
    public void testRestCreateRecord() {
        RegRecord newRecord = restCreateRecord();

        // ověření
        Assert.assertNotNull(newRecord);
        Assert.assertNotNull(newRecord.getRecordId());
        Assert.assertNotNull(recordRepository.getOne(newRecord.getRecordId()));
    }

    /**
     * Update záznamu v rejstříku - jiný popis, jiný typ.
     */
    @Test
    public void testRestUpdateRecord() {
        RegRecord record = restCreateRecord();

        Assert.assertTrue("Původní jméno", record.getRecord().equals(TEST_NAME));
        record.setRecord(TEST_UPDATE_NAME);

        RegRegisterType newRegisterType = createRegisterType();
        record.setRegisterType(newRegisterType);

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(record)
                        .put(UPDATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecord updatedRecord = response.getBody().as(RegRecord.class);

        // ověření
        Assert.assertNotNull(updatedRecord);
        Assert.assertTrue(updatedRecord.getRecord().equals(TEST_UPDATE_NAME));
        Assert.assertTrue(updatedRecord.getRegisterType().getRegisterTypeId().equals(newRegisterType.getRegisterTypeId()));
    }

    /**
     * Test smazání záznamu v rejstříku včetně návazných entit.
     */
    @Test
    public void testRestDeleteRecord() {
        RegRecord record = createRecord();

        long countStart = recordRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(RECORD_ID_ATT, record.getRecordId())
                .get(DELETE_RECORD_URL);
        logger.info(response.asString());
        long countEnd = recordRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);
    }

    /**
     * Vytvoření variantního záznamu rejstříku.
     */
    @Test
    public void testRestCreateVariantRecord() {
        RegRecord record = restCreateRecord();
        RegVariantRecord newVariantRecord = restCreateVariantRecord(record);

        // ověření
        Assert.assertNotNull(newVariantRecord);
        Assert.assertNotNull(newVariantRecord.getVariantRecordId());
        Assert.assertNotNull(variantRecordRepository.getOne(newVariantRecord.getVariantRecordId()));
    }

    /**
     * Update variantního záznamu v rejstříku - jiný popis, jiný typ.
     */
    @Test
    public void testRestUpdateVariantRecord() {
        RegRecord record = restCreateRecord();

        RegVariantRecord variantRecord = restCreateVariantRecord(record);

        Assert.assertTrue("Původní jméno", variantRecord.getRecord().equals(TEST_NAME));
        variantRecord.setRecord(TEST_UPDATE_NAME);

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(variantRecord)
                        .parameter(RECORD_ID_ATT, record.getId())
                        .put(UPDATE_VARIANT_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(response.print(), 200, response.statusCode());

        RegVariantRecord updatedVariantRecord = response.getBody().as(RegVariantRecord.class);

        // ověření
        Assert.assertNotNull(updatedVariantRecord);
        Assert.assertTrue(updatedVariantRecord.getRecord().equals(TEST_UPDATE_NAME));
    }

    /**
     * Test smazání variantních záznamů v rejstříku.
     */
    @Test
    public void testRestDeleteVariantRecord() {
        RegRecord record = createRecord();
        RegVariantRecord variantRecord = createVariantRecord(TEST_NAME, record);

        long countStart = variantRecordRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(VARIANT_RECORD_ID_ATT, variantRecord.getVariantRecordId())
                .get(DELETE_VARIANT_RECORD_URL);
        logger.info(response.asString());
        long countEnd = variantRecordRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);
    }

    /**
     * Test načtení typů rejstříkových záznamů - 2ks.
     */
    @Test
    public void testRestGetRegisterTypes() {
        createRegisterType();
        createRegisterType();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_REGISTER_TYPES_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RegRegisterType> registerTypes = Arrays.asList(response.getBody().as(RegRegisterType[].class));
        Assert.assertTrue("Nenalezeny typy záznamů (" + registerTypes.size() + ")", registerTypes.size() >= 2);
    }

    /**
     * Test načtení texterních zdrojů - 2ks.
     */
    @Test
    public void testRestGetExternalSources() {
        createExternalSource();
        createExternalSource();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_EXTERNAL_SOURCES_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RegExternalSource> externalSources = Arrays.asList(response.getBody().as(RegExternalSource[].class));
        Assert.assertTrue("Nenalezeny externí zdroje (" + externalSources.size() + ")", externalSources.size() >= 2);
    }

    /**
     * Test vyhledání záznamů v rejstříku dle hledacího řetězce - prohldávání i dle variantních záznamů.
     */
    @Test
    public void testRestFindRecords() {
        RegRecord record = createRecord();
        createVariantRecord("varianta", record);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, TEST_NAME)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RegRecord> records = Arrays.asList(response.getBody().as(RegRecord[].class));
        Assert.assertEquals("Nenalezena polozka: " + TEST_NAME, 1, records.size());
        Assert.assertEquals("Nenalezena variantní polozka: " + TEST_NAME, 1, records.get(0).getVariantRecordList().size());

        createVariantRecord("varianta", record);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "varianta")
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        records = Arrays.asList(response.getBody().as(RegRecord[].class));
        Assert.assertEquals("Nenalezena polozka: " + "varianta", 1, records.size());
        Assert.assertEquals("Nenalezena variantní polozka: " + TEST_NAME, 2, records.get(0).getVariantRecordList().size());
    }

    /**
     * Vyhledání záznamů - varianta pro počet.
     * Jeden záznam - výstup jeden dle name.
     * Dva záznamy - výstup jeden dle "varianta".
     */
    @Test
    public void testRestFindRecordsCount() {
        RegRecord record = createRecord();
        createVariantRecord("varianta", record);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, TEST_NAME)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_COUNT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        long recordsCount = response.getBody().as(long.class);
        Assert.assertEquals("Nenalezena polozka: " + TEST_NAME, 1, recordsCount);

        createRecord();

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "varianta")
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_COUNT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        recordsCount = response.getBody().as(long.class);
        Assert.assertEquals("Nenalezena polozka: varianta", 1, recordsCount);
    }

    /**
     * Vrácení jednoho záznamu dle ID.
     */
    @Test
    public void testRestGetRecord() {
        RegRecord record = createRecord();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .param(RECORD_ID_ATT, record.getRecordId())
                .get(GET_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecord foundRecord = response.getBody().as(RegRecord.class);

        Assert.assertNotNull(foundRecord);
        Assert.assertTrue(foundRecord.getRecordId().equals(record.getRecordId()));
    }

    /**
     * Vytvoření jednoho externího zdroje.
     * @return  vytvořený objekt, zapsaný do db
     */
    private RegExternalSource createExternalSource() {
        RegExternalSource externalSource = new RegExternalSource();
        externalSource.setCode(TEST_CODE);
        externalSource.setName(TEST_NAME);
        externalSourceRepository.save(externalSource);
        return externalSource;
    }

    /**
     * Vytvoří RESTově záznam rejstříku.
     *
     * @return  záznam
     */
    private RegRecord restCreateRecord() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);

        RegRegisterType registerType = createRegisterType();
        Integer externalSourceId = null;
        regRecord.setRegisterType(registerType);

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                        .body(regRecord)
//                        .parameter(REGISTER_TYPE_ID_ATT, registerType.getId())
//                        .parameter(EXTERNAL_SOURCE_ID_ATT, externalSourceId)
                        .put(CREATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        return response.getBody().as(RegRecord.class);
    }

    /**
     * Vytvoří RESTově variantní záznam rejstříku.
     *
     * @param record   k tomuto záznamu
     * @return  variantní záznam
     */
    private RegVariantRecord restCreateVariantRecord(final RegRecord record) {
        RegVariantRecord variantRecord = new RegVariantRecord();
        variantRecord.setRecord(TEST_NAME);
        variantRecord.setRegRecord(record);

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                        .body(variantRecord)
                        .put(CREATE_VARIANT_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(response.print(), 200, response.statusCode());

        return response.getBody().as(RegVariantRecord.class);
    }

}
