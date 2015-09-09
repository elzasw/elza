package cz.tacr.elza.controller;

import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
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
    public void test() {
        RegRegisterType registerTypeArtWork = getRegisterTypeArtWork();

        RegRecord record = fillRecord(registerTypeArtWork, "H1", "Heslo H1");
        createRecord(record);
        record = fillRecord(registerTypeArtWork, "H2", "Heslo H2");
        createRecord(record);
        record = fillRecord(registerTypeArtWork, "H3", "Heslo H3");
        createRecord(record);

        // chybný čísleník
        record.getRegisterType().setRegisterTypeId(666);
        createRecordNegative(record);


    }

    /**
     * Vyhledá typ rejstříku - unmělecké dílo.
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
                        .config(RestAssuredConfig.newConfig().encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8")))
                        .body(regRecord)
                        .put(CREATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        return response.getBody().as(RegRecord.class);
    }

    /**
     * Ověří, že nastane chyba při vytváření záznamu v rejstříku.
     * @param regRecord naplněný objekt k založení
     */
    private void createRecordNegative(final RegRecord regRecord) {
        Response response =
                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                        .config(RestAssuredConfig.newConfig().encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8")))
                        .body(regRecord)
                        .put(CREATE_RECORD_URL);
        logger.info(response.asString());
        Assert.assertEquals(500, response.statusCode());
    }

}
