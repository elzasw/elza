package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.vo.RegRecordWithCount;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import ma.glasnost.orika.MapperFactory;


/**
 * Test pro operace s rejstříkem.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegistryManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());



    private static final String GET_EXTERNAL_SOURCES_URL = REGISTRY_MANAGER_URL + "/getExternalSources";
    private static final String EXTERNAL_SOURCE_ID_ATT = "externalSourceId";


    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private ExternalSourceRepository externalSourceRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory configVOMapper;


    /**
     * Vytvoření záznamu rejstříku.
     */
    @Test
    public void testRestCreateRecord() {
        RegRecord newRecord = restCreateRecord("KOD1");

        // ověření
        Assert.assertNotNull(newRecord);
        Assert.assertNotNull(newRecord.getRecordId());
        Assert.assertNotNull(recordRepository.getOne(newRecord.getRecordId()));
    }

    /**
     * Vytvoření, aktualizace a smazání rejstříkového hesla.
     */
    @Test
    public void testSaveUpdateDeleteRecord() {
        RegRecord record = createRecord2("SR1");
        record.setCharacteristics("abc");

        RegRecord updatedRecord = registryService.saveRecord(record, true);
        Assert.assertEquals(updatedRecord.getCharacteristics(), "abc");

        Integer recordId = updatedRecord.getRecordId();
        registryService.deleteRecord(updatedRecord, true);

        Assert.assertNull(recordRepository.findOne(recordId));
    }

    /**
     * Vytvoření, aktualizace a smazání variantního rejstříkového hesla.
     */
    @Test
    public void testSaveUpdateDeleteVariantRecord(){

        RegRecord record = createRecord2("SR1");
        //vytvoříme var. record
        RegVariantRecord variantRecord = createVariantRecord("1", record);
        Integer variantRecordId = variantRecord.getVariantRecordId();

        //změníme obsah
        variantRecord.setRecord("2");

        RegVariantRecord updatedVariant = registryService.saveVariantRecord(variantRecord);
        updatedVariant = variantRecordRepository.findOne(variantRecordId);
        Assert.assertEquals(updatedVariant.getRecord(), "2");

        //smazání var. record
        variantRecordRepository.delete(variantRecordId);
        Assert.assertNull(variantRecordRepository.findOne(variantRecordId));

        recordRepository.delete(record);
    }

    /**
     * Update záznamu v rejstříku - jiný popis, jiný typ.
     */
    @Test
    public void testRestUpdateRecord() {
        RegRecord record = restCreateRecord("KOD2");

        Assert.assertTrue("Původní jméno", record.getRecord().equals(TEST_NAME));
        record.setRecord(TEST_UPDATE_NAME);

        RegRegisterType newRegisterType = createRegisterType("KOD3", null);
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
        RegRecord record = createRecord("KOD4");
        final ParPartyType partyType = findPartyType();

        long countStart = recordRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(RECORD_ID_ATT, record.getRecordId())
                .delete(DELETE_RECORD_URL);
        logger.info(response.asString());
        long countEnd = recordRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);

        // neexistující
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(RECORD_ID_ATT, 874522214)
                .delete(DELETE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        // s vazbou na osobu, nesmí se smazat
        record = createRecord("KOD4x");
        createParParty(partyType, record, null);

        countStart = recordRepository.count();
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(RECORD_ID_ATT, record.getRecordId())
                .delete(DELETE_RECORD_URL);
        logger.info(response.asString());
        countEnd = recordRepository.count();
        Assert.assertEquals(500, response.statusCode());
        Assert.assertEquals(countStart, countEnd);

    }

    /**
     * Vytvoření variantního záznamu rejstříku.
     */
    @Test
    public void testRestCreateVariantRecord() {
        RegRecord record = restCreateRecord("KOD5");
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
        RegRecord record = restCreateRecord("KOD6");

        RegVariantRecord variantRecord = restCreateVariantRecord(record);

        Assert.assertTrue("Původní jméno", variantRecord.getRecord().equals(TEST_NAME));
        variantRecord.setRecord(TEST_UPDATE_NAME);

        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(variantRecord)
                        .parameter(RECORD_ID_ATT, record.getRecordId())
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
        RegRecord record = createRecord("KOD7");
        RegVariantRecord variantRecord = createVariantRecord(TEST_NAME, record);

        long countStart = variantRecordRepository.count();

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(VARIANT_RECORD_ID_ATT, variantRecord.getVariantRecordId())
                .delete(DELETE_VARIANT_RECORD_URL);
        logger.info(response.asString());
        long countEnd = variantRecordRepository.count();

        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(countStart, countEnd + 1);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).parameter(VARIANT_RECORD_ID_ATT, 1278544547)
                .delete(DELETE_VARIANT_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
    }

    /**
     * Test načtení typů rejstříkových záznamů - 2ks.
     */
    @Test
    public void testRestGetRegisterTypes() {
        createRegisterType("KOD8", null);
        createRegisterType("KOD9", null);

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
        createExternalSource("ES-KOD1");
        createExternalSource("ES-KOD2");

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
        createRecord2("KOD10");
        RegRecord record = createRecord("KOD11");
        createVariantRecord("varianta", record);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, TEST_NAME)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        RegRecordWithCount recordWithCount = response.getBody().as(RegRecordWithCount.class);

        Assert.assertEquals("Nenalezena polozka: " + TEST_NAME, 1, recordWithCount.getRecordList().size());
        Assert.assertEquals("Nenalezena variantní polozka: " + TEST_NAME, 1, recordWithCount.getRecordList().get(0).getVariantRecordList().size());

        createVariantRecord("varianta", record);

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "varianta")
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        recordWithCount = response.getBody().as(RegRecordWithCount.class);

        Assert.assertEquals("Nenalezena polozka: " + "varianta", 1, recordWithCount.getRecordList().size());
        Assert.assertEquals("Nenalezena variantní polozka: " + TEST_NAME, 2, recordWithCount.getRecordList().get(0).getVariantRecordList().size());

        // null
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        recordWithCount = response.getBody().as(RegRecordWithCount.class);
        Assert.assertEquals("Nenalezena polozka.", 2, recordWithCount.getRecordList().size());

        // prázdné
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "")
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .parameter(REGISTER_TYPE_ID_ATT, new Integer[] {})
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        recordWithCount = response.getBody().as(RegRecordWithCount.class);
        Assert.assertEquals("Nenalezena polozka.", 2, recordWithCount.getRecordList().size());

        // mixed, první atribut je null, další je
        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .parameter(REGISTER_TYPE_ID_ATT, new Integer[] {})
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        recordWithCount = response.getBody().as(RegRecordWithCount.class);
        Assert.assertEquals("Nenalezena polozka.", 2, recordWithCount.getRecordList().size());
    }

    /**
     * Vytvoření jednoho záznamu rejstříku defaultního typu.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRecord createRecord2(final String uniqueCode) {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord("XXX");
        regRecord.setCharacteristics("CHAR");
        regRecord.setLocal(false);
        regRecord.setRegisterType(createRegisterType(uniqueCode, null));

        return recordRepository.save(regRecord);
    }

    /**
     * Vyhledání záznamů - varianta pro počet.
     * Jeden záznam - výstup jeden dle name.
     * Dva záznamy - výstup jeden dle "varianta".
     */
    @Test
    public void testRestFindRecordsCount() {
        RegRecord record = createRecord("KOD12");
        createVariantRecord("varianta", record);

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, TEST_NAME)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        RegRecordWithCount recordWithCount = response.getBody().as(RegRecordWithCount.class);

        long recordsCount = recordWithCount.getCount();
        Assert.assertEquals("Nenalezena polozka: " + TEST_NAME, 1, recordsCount);

        createRecord("KOD13");

        response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, "varianta")
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, record.getRegisterType().getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
        recordWithCount = response.getBody().as(RegRecordWithCount.class);

        recordsCount = recordWithCount.getCount();
        Assert.assertEquals("Nenalezena polozka: varianta", 1, recordsCount);
    }

    /**
     * Vrácení jednoho záznamu dle ID.
     */
    @Test
    public void testRestGetRecord() {
        RegRecord record = createRecord("KOD14");

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .param(RECORD_ID_ATT, record.getRecordId())
                .get(GET_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecord foundRecord = response.getBody().as(RegRecord.class);

        Assert.assertNotNull(foundRecord);
        Assert.assertTrue(foundRecord.getRecordId().equals(record.getRecordId()));
    }

    @Test
    public void testFindByParentRecord(){
        RegRecord kod1 = createRecord("KOD1");
        RegRecord kod2 = createRecord2("KOD2");

        kod1.setParentRecord(kod2);
        recordRepository.save(kod1);

        List<RegRecord> byParentRecord = recordRepository.findByParentRecord(kod2);
        Assert.assertTrue(byParentRecord.size() > 0);
        Assert.assertEquals(byParentRecord.iterator().next(),kod1);
    }


    /**
     * Vytvoření jednoho externího zdroje.
     * @return  vytvořený objekt, zapsaný do db
     */
    private RegExternalSource createExternalSource(final String uniqueCode) {
        RegExternalSource externalSource = new RegExternalSource();
        externalSource.setCode(uniqueCode);
        externalSource.setName(TEST_NAME);
        externalSourceRepository.save(externalSource);
        return externalSource;
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
