package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Test kompletní funkčnosti rejstříku a osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class PartyRegistryUsecaseTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /* Číselník typů rejstříku. */
    private static final String ARTWORK = "ARTWORK";


    @Test
    public void testRegistry() {
        testVytvoreniHesel();
        testHledaniHesel();
    }

    /**
     * Vytvoří hesla a variantní hesla. 1x negativní test.
     */
    private void testVytvoreniHesel() {
        RegRegisterType registerTypeArtWork = getRegisterTypeArtWork();

        RegRecord record = fillRecord(registerTypeArtWork, "H1", "Heslo H1");
        RegRecord heslo1 = createRecord(record);
        record = fillRecord(registerTypeArtWork, "H2", "Heslo H2");
        RegRecord heslo2 = createRecord(record);
        record = fillRecord(registerTypeArtWork, "H3", "Heslo H3");
        createRecord(record);

        // chybný čísleník
        record.getRegisterType().setRegisterTypeId(0);
        createRecordNegative(record);

        RegVariantRecord variantRecord = fillVariantRecord(heslo1, "Variantní heslo H1V1");
        createVariantRecord(variantRecord);
        variantRecord = fillVariantRecord(heslo1, "Variantní heslo H1V2");
        createVariantRecord(variantRecord);
        variantRecord = fillVariantRecord(heslo2, "Variantní heslo H2V1");
        createVariantRecord(variantRecord);
    }

    /**
     * Kontrola vyhledání.
     * <br></br>Dle "heslo" - všechny 3 hesla.
     * <br></br>Dle "H1" - jedno heslo, dvě variantní.
     * <br></br>Dle "V1" - dvě hesla, H1 - dvě variantní, H2 - jendo variantní.
     * <br></br>
     * <br></br>Kontrola odpovídajících počtů.
     */
    private void testHledaniHesel() {
        RegRegisterType registerTypeArtWork = getRegisterTypeArtWork();

        List<RegRecord> hesla = findRecords("heslo", registerTypeArtWork);
        long heslaCount = findRecordsCount("heslo", registerTypeArtWork);
        Assert.assertEquals("Nenalezeny hesla. ", 3, hesla.size());
        Assert.assertEquals("Neodpovídá počet. ", 3, heslaCount);

        hesla = findRecords("h1", registerTypeArtWork);
        heslaCount = findRecordsCount("h1", registerTypeArtWork);
        Assert.assertEquals("Nenalezeny hesla. ", 1, hesla.size());
        Assert.assertEquals("Nenalezeny variantní hesla: ", 2, hesla.get(0).getVariantRecordList().size());
        Assert.assertEquals("Neodpovídá počet. ", 1, heslaCount);

        hesla = findRecords("v1", registerTypeArtWork);
        heslaCount = findRecordsCount("v1", registerTypeArtWork);
        Assert.assertEquals("Nenalezeny hesla. ", 2, hesla.size());
        Assert.assertEquals("Neodpovídá počet. ", 2, heslaCount);

        // kontrola variantních
        boolean passed1 = false;
        boolean passed2 = false;
        for (final RegRecord heslo : hesla) {
            if ("H1".equalsIgnoreCase(heslo.getRecord())) {
                Assert.assertEquals("Nenalezeny variantní hesla: ", 2, heslo.getVariantRecordList().size());
                passed1 = true;
            }

            if ("H2".equalsIgnoreCase(heslo.getRecord())) {
                Assert.assertEquals("Nenalezeno variantní heslo: ", 1, heslo.getVariantRecordList().size());
                passed2 = true;
            }
        }
        Assert.assertTrue("Nekonalo se.", passed1);
        Assert.assertTrue("Nekonalo se.", passed2);
    }

    private void testVytvoreniOsob() {

    }

    /**
     * Vyhledá typ rejstříku - umělecké dílo.
     * @return      typ
     */
    private RegRegisterType getRegisterTypeArtWork() {
        List<RegRegisterType> regRegisterTypes = restGetRegisterTypes();

        RegRegisterType registerTypeArtWork = null;
        for (final RegRegisterType rt : regRegisterTypes) {
            if (rt.getCode().equalsIgnoreCase(ARTWORK)) {
                registerTypeArtWork = rt;
                break;
            }
        }

        Assert.assertNotNull("Výběr typu rejstříku - umělecké dílo - selhal.", registerTypeArtWork);
        return registerTypeArtWork;
    }

    /**
     * Načtení typů rejstříkových záznamů.
     */
    private List<RegRegisterType> restGetRegisterTypes() {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_REGISTER_TYPES_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        return (List<RegRegisterType>) Arrays.asList(response.getBody().as(RegRegisterType[].class));
    }

    /**
     * Připraví a naplní záznam rejstříku.
     *
     * @param registerType      typ
     * @param heslo             heslo
     * @param podrobnyPopis     popis hesla rejstříku
     * @return                  naplněný objekt, neuložený
     */
    private RegRecord fillRecord(final RegRegisterType registerType, final String heslo, final String podrobnyPopis) {
        RegRecord record = new RegRecord();
        record.setRegisterType(registerType);
        record.setRecord(heslo);
        record.setCharacteristics(podrobnyPopis);
        record.setLocal(true);

        return record;
    }

    /**
     * Vytvoří záznam rejstříku.
     * @param regRecord naplněný objekt k založení
     * @return  záznam
     */
    private RegRecord createRecord(final RegRecord regRecord) {
        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                        .config(getUtf8Config())
                        .body(regRecord)
                        .put(CREATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecord newRecord = response.getBody().as(RegRecord.class);

        // ověření
        Assert.assertNotNull(newRecord);
        Assert.assertNotNull(newRecord.getRecordId());

        return newRecord;
    }

    /**
     * Ověří, že nastane chyba při vytváření záznamu v rejstříku.
     * @param regRecord naplněný objekt k založení
     */
    private void createRecordNegative(final RegRecord regRecord) {
        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                        .config(getUtf8Config())
                        .body(regRecord)
                        .put(CREATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(500, response.statusCode());
    }

    /**
     * Připraví a naplní záznam rejstříku.
     *
     * @param regRecord         k tomuto záznamu
     * @param heslo             heslo
     * @return                  naplněný objekt, neuložený
     */
    private RegVariantRecord fillVariantRecord(final RegRecord regRecord, final String heslo) {
        RegVariantRecord variantRecord = new RegVariantRecord();
        variantRecord.setRegRecord(regRecord);
        variantRecord.setRecord(heslo);

        return variantRecord;
    }

    /**
     * Vytvoří variantní záznam rejstříku.
     *
     * @param variantRecord   variantní heslo
     * @return  záznam
     */
    private RegVariantRecord createVariantRecord(final RegVariantRecord variantRecord) {
        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                        .config(getUtf8Config())
                        .body(variantRecord)
                        .put(CREATE_VARIANT_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(response.print(), 200, response.statusCode());

        RegVariantRecord newVariantRecord = response.getBody().as(RegVariantRecord.class);

        // ověření
        Assert.assertNotNull(newVariantRecord);
        Assert.assertNotNull(newVariantRecord.getVariantRecordId());

        return newVariantRecord;
    }

    /**
     * Najde hesla dle řetězce a typu.
     *
     * @param searchString      hledaný text
     * @param registerType      typ hesla
     * @return                  nalezená hesla
     */
    private List<RegRecord> findRecords(final String searchString, final RegRegisterType registerType) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, searchString)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .parameter(REGISTER_TYPE_ID_ATT, registerType.getRegisterTypeId())
                .get(FIND_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        return (List<RegRecord>) Arrays.asList(response.getBody().as(RegRecord[].class));
    }

    /**
     * Najde počet hesel dle řetězce a typu.
     *
     * @param searchString      hledaný text
     * @param registerType      typ hesla
     * @return                  počet nalezených
     */
    private long findRecordsCount(final String searchString, final RegRegisterType registerType) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, searchString)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 10)
                .parameter(REGISTER_TYPE_ID_ATT, registerType.getRegisterTypeId())
                .get(FIND_RECORD_COUNT_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        return response.getBody().as(long.class);
    }

}
