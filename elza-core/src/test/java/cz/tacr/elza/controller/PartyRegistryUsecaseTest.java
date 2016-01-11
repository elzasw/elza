package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.response.Response;

import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeExt;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.vo.ParPartyWithCount;
import cz.tacr.elza.domain.vo.RegRecordWithCount;

/**
 * Test kompletní funkčnosti rejstříku a osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class PartyRegistryUsecaseTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /* Číselník typů rejstříku. */
    private static final String ARTWORK = "ARTWORK";

    /* Číselník podtypů osoby. */
    private static final String FYZ_OSOBA = "PERSON";
    private static final String UDAL_PRIRODA = "EVENT";
    private static final String FIKT_ROD = "DYNASTY";

    /** Objekty testu. */
    private RegRecord heslo1;
    private RegRecord heslo2;
    private ParParty party1;

    private static final String VAR_HESLO_H1V1  = "Variantni heslo H1V1";
    private static final String VAR_HESLO_H1V1_UPDATE  = "Variantni heslo H1V1 aktualizace";


    @Test
    public void testRegistryAndParty() {
        testVytvoreniHesel();
        testHledaniHesel();
        testVytvoreniOsob();
        testHledaniOsob();
        testAktualizace();
        testSmazani();
    }

    /**
     * Vytvoří hesla a variantní hesla. 1x negativní test.
     */
    private void testVytvoreniHesel() {
        RegRegisterType registerTypeArtWork = getRegisterTypeArtWork();

        RegRecord record = fillRecord(registerTypeArtWork, "H1", "Heslo H1");
        heslo1 = createRecord(record);
        record = fillRecord(registerTypeArtWork, "H2", "Heslo H2");
        heslo2 = createRecord(record);
        record = fillRecord(registerTypeArtWork, "H3", "Heslo H3");
        createRecord(record);

        // chybný čísleník
        record.getRegisterType().setRegisterTypeId(0);
        createRecordNegative(record);

        RegVariantRecord variantRecord = fillVariantRecord(heslo1, VAR_HESLO_H1V1);
        createVariantRecord(variantRecord);
        variantRecord = fillVariantRecord(heslo1, "Variantni heslo H1V2");
        createVariantRecord(variantRecord);
        variantRecord = fillVariantRecord(heslo2, "Variantni heslo H2V1");
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
        List<RegRecordVO> heslaVO = findRecordsRest("heslo", registerTypeArtWork);

        long heslaCount = findRecordsCount("heslo", registerTypeArtWork);
        long heslaCountRest = findRecordsCountRest("heslo", registerTypeArtWork);
        Assert.assertEquals(heslaCount, heslaCountRest);
        Assert.assertEquals("Nenalezeny hesla. ", 3, hesla.size());
        Assert.assertEquals("Neodpovídá počet. ", 3, heslaCount);
        Assert.assertEquals("Jine hledani pres rest", hesla.size(), heslaVO.size());



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

    /**
     * Vytvoří abstractní osoby různých typů připojených k heslům rejstříku.
     */
    private void testVytvoreniOsob() {
        ParPartyType partySubTypeOsoba = getPartyType(FYZ_OSOBA);
        ParPartyType partySubTypeUdalost = getPartyType(UDAL_PRIRODA);

        ParPartyName preferredNameOsoba = new ParPartyName();
        ParParty party = fillParty(partySubTypeOsoba, heslo1, preferredNameOsoba);
        party1 = createParty(party);

        ParPartyName preferredNameUdalost = new ParPartyName();
        party = fillParty(partySubTypeUdalost, heslo2, preferredNameUdalost);
        createParty(party);
    }

    /**
     * Vyhledání osob dle výskytu řetězce v heslech.
     */
    private void testHledaniOsob() {
        ParPartyType partySubTypeOsoba = getPartyType(FYZ_OSOBA);
        ParPartyType partySubTypeUdalost = getPartyType(UDAL_PRIRODA);

        List<ParParty> osoby = findParty("h1", partySubTypeOsoba, true);
        long osobyCount = findPartyCount("h1", partySubTypeOsoba, true);
        Assert.assertEquals("Nenalezeny osoby. ", 1, osoby.size());
        Assert.assertTrue("Není očekávané heslo.", osoby.get(0).getRecord().equals(heslo1));
        Assert.assertEquals("Neodpovídá počet. ", 1, osobyCount);

        osoby = findParty("H1", partySubTypeOsoba, false);
        osobyCount = findPartyCount("H1", partySubTypeOsoba, false);
        Assert.assertEquals("Nalezeny osoby. ", 0, osoby.size());
        Assert.assertEquals("Neodpovídá počet. ", 0, osobyCount);

//        osoby = findParty("V1", partySubTypeUdalost, false);
//        osobyCount = findPartyCount("V1", partySubTypeUdalost, false);
//        Assert.assertEquals("Nenalezeny osoby. ", 1, osoby.size());
//        Assert.assertTrue("Není očekávané heslo.", osoby.get(0).getRecord().equals(heslo2));
//        Assert.assertEquals("Neodpovídá počet. ", 1, osobyCount);
    }

    /**
     * Otestuje aktualizace objektů. Včetně 1x konkurentní přístup.
     */
    private void testAktualizace() {
        // osoba, její podtyp
        ParPartyType subTypeFiktRod = getPartyType(FIKT_ROD);
        party1.setPartyType(subTypeFiktRod);
        updateParty(party1);
        ParParty ap = getParty(party1.getPartyId());
        Assert.assertEquals("Update neproveden.", subTypeFiktRod, ap.getPartyType());

        // heslo, jeho podrobný popis
        heslo1.setCharacteristics("Heslo H111");
        updateRecord(heslo1);
        heslo1 = getRecord(heslo1.getRecordId());
        RegRecordVO heslo1Vo = getRecordRest(heslo1.getRecordId());
        Assert.assertEquals(heslo1.getRecordId(), heslo1Vo.getRecordId());

        Assert.assertEquals("Update neproveden.", "Heslo H111", heslo1.getCharacteristics());

        // variantní heslo, jeho heslo
        RegVariantRecord variantRecord = getVariantniHesloByRecord(heslo1, VAR_HESLO_H1V1);
        variantRecord.setRecord(VAR_HESLO_H1V1_UPDATE);
        heslo1.getVariantRecordList().clear();
        variantRecord.setRegRecord(heslo1);
        updateVariantRecord(variantRecord);
        heslo1 = getRecord(heslo1.getRecordId());
        variantRecord = getVariantniHesloByRecord(heslo1, VAR_HESLO_H1V1_UPDATE);
        Assert.assertEquals("Update neproveden.", VAR_HESLO_H1V1_UPDATE, variantRecord.getRecord());

        // concurrent modification, heslo 2, 2x get, změna - update, druhý exemplář změna - update -> CHYBA
        heslo2 = getRecord(heslo2.getRecordId());
        RegRecord heslo2a = getRecord(heslo2.getRecordId());
        heslo2.setRecord("H2x");
        updateRecord(heslo2);
        heslo2a.setRecord("H2y");
        updateRecordConcurrentError(heslo2a);
    }

    /**
     * Test smazání objektů.
     */
    private void testSmazani() {
        delete(party1.getPartyId(), ABSTRACT_PARTY_ID_ATT, DELETE_ABSTRACT_PARTY);
        Assert.assertNotNull(getRecord(heslo1.getRecordId())); // zůstává rej. heslo
        getErr(party1.getPartyId(), ABSTRACT_PARTY_ID_ATT, GET_ABSTRACT_PARTY);

        delete(heslo1.getRecordId(), RECORD_ID_ATT, DELETE_RECORD_URL);
        getErr(heslo1.getRecordId(), RECORD_ID_ATT, GET_RECORD_URL);

        RegVariantRecord vr = getVariantniHesloByRecord(heslo1, VAR_HESLO_H1V1_UPDATE);
        delete(vr.getVariantRecordId(), VARIANT_RECORD_ID_ATT, DELETE_VARIANT_RECORD_URL);
    }

    /**
     * Najde variantní záznam z předaného rejstříkového hesla.
     * @param regRecord     heslo
     * @param record        text variantního hesla, které hledáme po nadřazeným heslem
     * @return              var. heslo
     */
    private RegVariantRecord getVariantniHesloByRecord(final RegRecord regRecord, final String record) {
        RegVariantRecord variantRecord = null;
        for (final RegVariantRecord vr : regRecord.getVariantRecordList()) {
            if (record.equals(vr.getRecord())) {
                variantRecord = vr;
            }
        }

        Assert.assertNotNull(variantRecord);
        return variantRecord;
    }

    /**
     * Vyhledá typ rejstříku - umělecké dílo.
     * @return      typ
     */
    private RegRegisterType getRegisterTypeArtWork() {
        List<RegRegisterType> regRegisterTypes = getRegisterTypes();

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
    private List<RegRegisterType> getRegisterTypes() {
        Response response = get((spec) -> spec, GET_REGISTER_TYPES_URL);
        return (List<RegRegisterType>) Arrays.asList(response.getBody().as(RegRegisterType[].class));
    }

    /**
     * Vyhledá podtyp osoby dle kódu.
     *
     * @return      podtyp
     */
    private ParPartyType getPartyType(final String code) {
        List<ParPartyTypeExt> partyTypes = getPartyTypes();

        ParPartyType result = null;
        for (final ParPartyTypeExt pt : partyTypes) {
            if (pt.getCode().equalsIgnoreCase(code)) {
                result = pt;
                break;
            }
        }

        Assert.assertNotNull("Výběr typu rejstříku - umělecké dílo - selhal.", result);
        return result;
    }

    public List<ParPartyTypeExt> getPartyTypes() {
        Response response = get((spec) -> spec, GET_PARTY_TYPES);
        return (List<ParPartyTypeExt>) Arrays.asList(response.getBody().as(ParPartyTypeExt[].class));
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
        Response response = put((spec) -> spec
                        .body(regRecord),
                CREATE_RECORD_URL
        );

        RegRecord newRecord = response.getBody().as(RegRecord.class);

        // ověření
        Assert.assertNotNull(newRecord);
        Assert.assertNotNull(newRecord.getRecordId());
        Assert.assertEquals(regRecord.getRecord() , newRecord.getRecord());
        Assert.assertEquals(regRecord.getCharacteristics() , newRecord.getCharacteristics());

        return newRecord;
    }

    /**
     * Ověří, že nastane chyba při vytváření záznamu v rejstříku.
     * @param regRecord naplněný objekt k založení
     */
    private void createRecordNegative(final RegRecord regRecord) {
        putError((spec) -> spec
                        .body(regRecord),
                CREATE_RECORD_URL
        );
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
        Response response = put((spec) -> spec
                        .body(variantRecord)
                , CREATE_VARIANT_RECORD_URL
        );

        RegVariantRecord newVariantRecord = response.getBody().as(RegVariantRecord.class);

        // ověření
        Assert.assertNotNull(newVariantRecord);
        Assert.assertNotNull(newVariantRecord.getVariantRecordId());
        Assert.assertEquals(variantRecord.getRecord(), newVariantRecord.getRecord());

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
        Set<Integer> typeIds = null;
        if(registerType != null){
            typeIds = new HashSet<>();
            typeIds.add(registerType.getRegisterTypeId());
        }

        return registryService.findRegRecordByTextAndType(searchString, typeIds, null, 0, 10, null);
    }

    private List<RegRecordVO> findRecordsRest(final String searchString, final RegRegisterType registerType) {
        Response response = get((spec) -> spec
                        .parameter(SEARCH_ATT, searchString)
                        .parameter(FROM_ATT, 0)
                        .parameter(COUNT_ATT, 10)
                        .parameter(REGISTER_TYPE_ID_ATT, registerType.getRegisterTypeId())
                , FIND_RECORD_URL_V2
        );

        cz.tacr.elza.controller.vo.RegRecordWithCount recordWithCount = response.getBody()
                .as(cz.tacr.elza.controller.vo.RegRecordWithCount.class);
        return recordWithCount.getRecordList();
    }

    /**
     * Najde počet hesel dle řetězce a typu.
     *
     * @param searchString hledaný text
     * @param registerType typ hesla
     * @return počet nalezených
     */
    private long findRecordsCount(final String searchString, final RegRegisterType registerType) {
        Set<Integer> typeIds = null;
        if (registerType != null) {
            typeIds = new HashSet<>();
            typeIds.add(registerType.getRegisterTypeId());
        }

        return registryService.findRegRecordByTextAndTypeCount(searchString, typeIds, null, null);
    }

    /**
     * Najde počet hesel dle řetězce a typu.
     *
     * @param searchString      hledaný text
     * @param registerType      typ hesla
     * @return                  počet nalezených
     */
    private long findRecordsCountRest(final String searchString, final RegRegisterType registerType) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, searchString)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 1)
                .parameter(REGISTER_TYPE_ID_ATT, registerType.getRegisterTypeId())
                .get(FIND_RECORD_URL_V2);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        RegRecordWithCount recordWithCount = response.getBody().as(RegRecordWithCount.class);
        return recordWithCount.getCount();
    }

    /**
     * Naplní entitu osoby.
     *
     * @param partySubtype      podtyp
     * @param regRecord         heslo ke kterému bude připojena
     * @return  naplněný objekt, neuložený
     */
    private ParParty fillParty(final ParPartyType partySubtype, final RegRecord regRecord, final ParPartyName partyName) {
        ParParty party = new ParParty();
        party.setPartyType(partySubtype);
        party.setRecord(regRecord);
        party.setPreferredName(partyName);

        return party;
    }

    /**
     * Vytvoří abstraktní osobu.
     *
     * @param party   objekt osoby
     * @return  záznam
     */
    private ParParty createParty(final ParParty party) {
        Response response = put((spec) -> spec
                        .body(party),
                        INSERT_ABSTRACT_PARTY
        );

        ParParty newParty = response.getBody().as(ParParty.class);

        // ověření
        Assert.assertNotNull(newParty);
        Assert.assertNotNull(newParty.getPartyId());
        Assert.assertNotNull("Nenalezena polozka party type", newParty.getPartyType());
        Assert.assertNotNull("Nenalezena polozka record", newParty.getRecord());
        Assert.assertEquals("Nenalezena spravna polozka record", party.getRecord(), newParty.getRecord());
        Assert.assertEquals("Nenalezena spravna polozka subtype", party.getPartyType(), newParty.getPartyType());

        return newParty;
    }

    /**
     * Nalezne abstraktní osoby dle parametrů.
     * @param searchString      řetězec hledání
     * @param partyType         typ osoby
     * @return                  entity
     */
    private List<ParParty> findParty(final String searchString, final ParPartyType partyType,
                                                     final boolean originator) {

        Response response = get((spec) -> spec
                        .parameter(SEARCH_ATT, searchString)
                        .parameter(FROM_ATT, 0)
                        .parameter(COUNT_ATT, 10)
                        .parameter(PARTY_TYPE_ID_ATT, partyType.getPartyTypeId())
                        .parameter(ORIGINATOR_ATT, originator),
                FIND_ABSTRACT_PARTY
        );

        ParPartyWithCount partyWithCount = response.getBody().as(ParPartyWithCount.class);
        return partyWithCount.getRecordList();
    }

    /**
     * Nalezne počet abstraktních osob dle parametrů.
     * @param searchString      řetězec hledání
     * @param partyType         typ osoby
     * @return                  počet entit
     */
    private long findPartyCount(final String searchString, final ParPartyType partyType,
                                        final boolean originator) {

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(SEARCH_ATT, searchString)
                .parameter(FROM_ATT, 0)
                .parameter(COUNT_ATT, 0)
                .parameter(PARTY_TYPE_ID_ATT, partyType.getPartyTypeId())
                .parameter(ORIGINATOR_ATT, originator)
                .get(FIND_ABSTRACT_PARTY);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        ParPartyWithCount partyWithCount = response.getBody().as(ParPartyWithCount.class);
        return partyWithCount.getCount();
    }

    /**
     * Upraví abstraktní osobu.
     *
     * @param party   objekt osoby
     * @return  záznam
     */
    private ParParty updateParty(final ParParty party) {
        Response response = put((spec) -> spec
                        .body(party),
                UPDATE_ABSTRACT_PARTY
        );

        ParParty updatedParty = response.getBody().as(ParParty.class);

        // ověření
        Assert.assertNotNull(updatedParty);
        Assert.assertNotNull(updatedParty.getPartyId());
        Assert.assertNotNull("Nenalezena polozka party subtype", updatedParty.getPartyType());
        Assert.assertNotNull("Nenalezena polozka record", updatedParty.getRecord());
        Assert.assertEquals("Nenalezena spravna polozka record", party.getRecord(), updatedParty.getRecord());
        Assert.assertEquals("Nenalezena spravna polozka subtype", party.getPartyType(), updatedParty.getPartyType());

        return updatedParty;
    }

    /**
     * Upraví rejstříkové heslo.
     *
     * @param regRecord   objekt
     * @return  záznam
     */
    private RegRecord updateRecord(final RegRecord regRecord) {
        Response response = put((spec) -> spec
                        .body(regRecord),
                UPDATE_RECORD_URL
        );

        RegRecord updatedRecord = response.getBody().as(RegRecord.class);

        // ověření
        Assert.assertNotNull(updatedRecord);
        Assert.assertNotNull(updatedRecord.getRecordId());
        Assert.assertNotNull("Nenalezena polozka typu", updatedRecord.getRegisterType());
        Assert.assertEquals("Nenalezena spravna polozka typu", regRecord.getRegisterType(), updatedRecord.getRegisterType());

        return updatedRecord;
    }

    /**
     * Prověří konkurent modification.
     *
     * @param regRecord   objekt
     */
    private void updateRecordConcurrentError(final RegRecord regRecord) {
        RestAssuredResponseImpl response = (RestAssuredResponseImpl) putError((spec) -> spec
                        .body(regRecord),
                UPDATE_RECORD_URL
        );

        Assert.assertTrue("Nefunguje optimistic locking.",
                StringUtils.contains((String)response.getContent(), ConcurrentUpdateException.class.getName())
        );
    }

    /**
     * Upraví variantní rejstříkové heslo.
     *
     * @param regVariantRecord   objekt
     * @return  záznam
     */
    private RegVariantRecord updateVariantRecord(final RegVariantRecord regVariantRecord) {
        Response response = put((spec) -> spec
                        .body(regVariantRecord),
                UPDATE_VARIANT_RECORD_URL
        );

        RegVariantRecord updatedVariantRecord = response.getBody().as(RegVariantRecord.class);

        // ověření
        Assert.assertNotNull(updatedVariantRecord);
        Assert.assertNotNull(updatedVariantRecord.getVariantRecordId());
        Assert.assertNotNull("Nenalezena polozka hesla", updatedVariantRecord.getRegRecord());
        Assert.assertEquals("Nenalezena spravna polozka hesla", regVariantRecord.getRegRecord(),
                updatedVariantRecord.getRegRecord());

        return updatedVariantRecord;
    }

    /**
     * @param partyId   id pro načtení
     * @return      entita
     */
    private ParParty getParty(final Integer partyId) {
        Response response = get((spec) -> spec
                        .parameter(ABSTRACT_PARTY_ID_ATT, partyId),
                GET_ABSTRACT_PARTY
        );

        return response.getBody().as(ParParty.class);
    }

    /**
     * @param recordId   id pro načtení
     * @return      entita
     */
    private RegRecord getRecord(final Integer recordId) {

        return recordRepository.getOne(recordId);
    }

    private RegRecordVO getRecordRest(final Integer recordId) {
        Response response = get((spec) -> spec
                        .parameter(RECORD_ID_ATT, recordId),
                GET_RECORD_URL_V2
        );

        return response.getBody().as(RegRecordVO.class);
    }


    /**
     * Smazání libovolného záznamu dle id.
     *
     * @param id            id záznamu
     * @param attribute     jméno atributu k plnění
     * @param deleteUrl     url
     */
    private void delete(final Integer id, final String attribute, final String deleteUrl) {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .parameter(attribute, id)
                .delete(deleteUrl);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
    }

    /**
     * Get neexistujícícho záznamu. Projde-li, záznam skutečně neexistuje.
     *
     * @param id            id záznamu
     * @param attribute     jméno atributu k plnění
     * @param getUrl        url
     */
    private void getErr(final Integer id, final String attribute, final String getUrl) {
        getError((spec) -> spec
                        .parameter(attribute, id),
                getUrl
        );
    }

}
