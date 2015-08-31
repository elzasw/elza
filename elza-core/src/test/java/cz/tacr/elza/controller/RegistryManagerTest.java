package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.AbstractPartyRepository;
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
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegistryManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CREATE_RECORD_URL = REGISTRY_MANAGER_URL + "/createRecord";
    private static final String DELETE_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteRecord";
    private static final String DELETE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteVariantRecord";
    private static final String FIND_RECORD_URL = REGISTRY_MANAGER_URL + "/findRecord";
    private static final String FIND_RECORD_COUNT_URL = REGISTRY_MANAGER_URL + "/findRecordCount";
    private static final String GET_REGISTER_TYPES_URL = REGISTRY_MANAGER_URL + "/getRegisterTypes";
    private static final String GET_EXTERNAL_SOURCES_URL = REGISTRY_MANAGER_URL + "/getExternalSources";
    private static final String GET_RECORD_URL = REGISTRY_MANAGER_URL + "/getRecord";

    private static final String RECORD_ATT = "record";
    private static final String RECORD_ID_ATT = "recordId";
    private static final String VARIANT_RECORD_ID_ATT = "variantRecordId";

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

    @Autowired
    private ExternalSourceRepository externalSourceRepository;


    /**
     * Vytvoření záznamu rejstříku.
     */
    @Test
    public void testRestCreateRecord() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);

        RegRegisterType registerType = createRegisterType();

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(regRecord)
                        .post(CREATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecord newRecord = response.getBody().as(RegRecord.class);

        // ověření
        Assert.assertNotNull(recordRepository.getOne(newRecord.getId()));
        Assert.assertNotNull(newRecord);
        Assert.assertNotNull(newRecord.getRecordId());
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

    /**
     * Test smazání záznamu v rejstříku včetně návazných entit.
     */
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

    /**
     * Test smazání variantních záznamů v rejstříku.
     */
    @Test
    public void testRestDeleteVariantRecord() {
        RegRecord record = createRecord();
        RegVariantRecord variantRecord = createVariantRecord(TEST_NAME, record);

        long countStart = variantRecordRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(VARIANT_RECORD_ID_ATT, variantRecord.getId())
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
        Assert.assertTrue("Nenalezeny typy záznamů.", registerTypes.size() == 2);
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
        Assert.assertTrue("Nenalezeny externí zdroje.", externalSources.size() == 2);
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
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<RegRecord> records = Arrays.asList(response.getBody().as(RegRecord[].class));
        Assert.assertTrue("Nenalezena polozka: " + TEST_NAME, records.size() == 1);
        Assert.assertTrue("Nenalezena variantní polozka: " + TEST_NAME, records.get(0).getVariantRecordList().size() == 1);

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
        Assert.assertTrue("Nenalezena variantní polozka: " + TEST_NAME, records.get(0).getVariantRecordList().size() == 2);
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
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getId())
                .get(FIND_RECORD_COUNT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        long recordsCount = response.getBody().as(long.class);
        Assert.assertTrue("Nenalezena polozka: " + TEST_NAME, recordsCount == 1);

        createRecord();

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "varianta")
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getId())
                .get(FIND_RECORD_COUNT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        recordsCount = response.getBody().as(long.class);
        Assert.assertTrue("Nenalezena polozka: varianta", recordsCount == 1);
    }

    /**
     * Vrácení jednoho záznamu dle ID.
     */
    @Test
    public void testRestGetRecord() {
        RegRecord record = createRecord();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .param(RECORD_ID_ATT, record.getId())
                .get(GET_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecord foundRecord = response.getBody().as(RegRecord.class);

        Assert.assertNotNull(foundRecord);
        Assert.assertTrue(foundRecord.getId().equals(record.getId()));
    }

    /**
     * Vytvoření jednoho typu rejstříku.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRegisterType createRegisterType() {
        RegRegisterType regRegisterType = new RegRegisterType();
        regRegisterType.setCode(TEST_CODE);
        regRegisterType.setName(TEST_NAME);
        registerTypeRepository.save(regRegisterType);
        return regRegisterType;
    }

    /**
     * Vytvoření jednoho externího zdroje.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegExternalSource createExternalSource() {
        RegExternalSource externalSource = new RegExternalSource();
        externalSource.setCode(TEST_CODE);
        externalSource.setName(TEST_NAME);
        externalSourceRepository.save(externalSource);
        return externalSource;
    }

    /**
     * Vytvoření jednoho záznamu rejstříku defaultního typu.
     * @return  vytvořený objekt, zapsaný do db
     */
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
