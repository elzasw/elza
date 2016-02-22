package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author Petr Compel
 * @since 19.2.2016
 */
public class PartyControllerTest extends AbstractControllerTest {

    private static final String PERSON_TYPE_CODE = "PERSON";
    private static final String GROUP_PARTY_TYPE_CODE = "GROUP_PARTY";
    private static final String DYNASTY_TYPE_CODE = "DYNASTY";
    private static final String EVENT_TYPE_CODE = "EVENT";

    private static final String PRIMARY_NAME_TYPE_CODE = "1";

    /**
     * Scénář - obecně vyplňovat políčka, které jsou na formulářích; všechny akce jsou založené na aktivitě uživatele přímo v aplikaci
     * ----
     * - Vytvořit novou osobu "O1"
     * - Vytvořit novou korporaci "K1"
     * - Vytvořit nový rod "R1"
     * - Vytvořit novou dočasnou korporaci (událost) "U1"
     * - Získat seznam všech osob, ověřit výsledek (existují 4 osoby, ke každé z nich 1 preferované jméno a 1 rejstříkový záznam)
     * - Získat seznam osob s nějakým filtrem, ověřit výsledek
     * - Přidat k osobě O1 nové jméno (včetně 2 doplňků jména)
     * - Nastavit preferované jméno osoby O1 na to nově přidané
     * - Upravit osobu O1 - změnit charakteristiku, dějiny, poznámku, zdroje informací
     * - Získat detail osoby O1, ověřit výsledek
     * - Upravit rod R1, vyplnit genealogii rodu
     * - Vyplnit autora rodu R1 (přidej 2 autory - rod R1 a osobu O1, smaž ze seznamu autorů rod R1)
     * - Získat detail rodu R1, ověřit výsledek
     * - Upravit korporaci K1 - změnit Vnitřní struktury, Konstitutivní norma, Normy působnosti, Funkce korporace
     * - Přidat 2 nové identifikátory korporace K1, jeden z nich smaž
     * - Získat detail korporace K1, ověřit výsledek
     * - Přidat nové rejstříkové heslo GEO1 do geografického rejstříku
     * - Přidat k osobě O1 vztah třídy vznik, typ vznik osoby, vyplnit datace apod., vztah k entitě typu místo narození, entita GEO1
     * - Přidat k osobě O1 vztah třídy zánik, typ zánik osoby, vyplnit datace apod., vztah k entitě nebude žádný (neznámé místo úmrtí...)
     * - Přidat k osobě O1 vztah třídy vztah, typ zaměstnání, vyplnit datace apod., vztah k entitě typu zaměstnavatel/zaměstnavatelka, entita K1
     * - Získat detail osoby O1, ověřit výsledek
     * - Úprava vztahu zaměstnání - data
     * - Ověření
     * - Odstranění vztahu zaměstnání
     * - Vyhledávání osob podle osoby (zkontroluje zda existuje 1 podle 1 aktuální, vytvoří osobu a vyhledá jestli jsou 2)
     * - Smazat dočasnou korporaci U1
     * - Získat seznam všech osob, ověřit výsledek (existují 3 osoby)
     */
    @Test
    public void scenarioTest() {
        /** Testovací datumy **/
        ArrCalendarTypeVO gregorian = findCalendarByCode(calendarTypes(), "GREGORIAN");

        ParUnitdateVO testFromDate = new ParUnitdateVO();
        testFromDate.setCalendarTypeId(gregorian.getId());
        testFromDate.setTextDate("2.2.2012");
        ParUnitdateVO testToDate = new ParUnitdateVO();
        testToDate.setCalendarTypeId(gregorian.getId());
        testToDate.setTextDate("2.3.2012");

        /** Číselníky **/
        List<ParPartyTypeVO> partyTypes = getPartyTypes();
        List<ParPartyNameFormTypeVO> nameTypes = getPartyNameFormTypes();

        RegScopeVO scope = faScopes().iterator().next();

        ParPartyTypeVO typeO1 = findPartyTypeByCode(partyTypes, PERSON_TYPE_CODE);
        ParPartyTypeVO typeK1 = findPartyTypeByCode(partyTypes, GROUP_PARTY_TYPE_CODE);
        ParPartyTypeVO typeR1 = findPartyTypeByCode(partyTypes, DYNASTY_TYPE_CODE);
        ParPartyTypeVO typeU1 = findPartyTypeByCode(partyTypes, EVENT_TYPE_CODE);
        Assert.assertNotNull("Očekáváme základní typy osob", typeO1);
        Assert.assertNotNull("Očekáváme základní typy osob", typeK1);
        Assert.assertNotNull("Očekáváme základní typy osob", typeR1);
        Assert.assertNotNull("Očekáváme základní typy osob", typeU1);
        ParPartyNameFormTypeVO typePrimaryName = findPartyNameFormByCode(nameTypes, PRIMARY_NAME_TYPE_CODE);

        /** Vytvoření osoby **/
        ParPersonVO personO1 = new ParPersonVO();
        RegRecordVO recordO1 = new RegRecordVO();

        recordO1.setRegisterTypeId(findRegisterTypeAddable(recordTypesForPartyType(typeO1.getPartyTypeId())).getId());
        recordO1.setScopeId(scope.getId());
        personO1.setRecord(recordO1);
        personO1.setPartyType(typeO1);

        ParPartyNameVO partyNameO1 = new ParPartyNameVO();
        partyNameO1.setDegreeAfter("O1 After");
        partyNameO1.setDegreeBefore("O1 Before");
        partyNameO1.setOtherPart("O1 Other");
        partyNameO1.setNote("O1 Note");
        partyNameO1.setDisplayName("O1");
        partyNameO1.setMainPart("O1");
        partyNameO1.setPrefferedName(true);

        partyNameO1.setNameFormType(typePrimaryName);
        personO1.setPartyNames(Collections.singletonList(partyNameO1));
        personO1.setFrom(testFromDate);
        personO1.setTo(testToDate);

        personO1 = (ParPersonVO) insertParty(personO1);


        /** Vytvoření korporace **/
        ParPartyGroupVO groupK1 = new ParPartyGroupVO();
        RegRecordVO recordK1 = new RegRecordVO();

        recordK1.setRegisterTypeId(findRegisterTypeAddable(recordTypesForPartyType(typeK1.getPartyTypeId())).getId());
        recordK1.setScopeId(scope.getId());

        groupK1.setRecord(recordK1);

        groupK1.setPartyType(typeK1);
        groupK1.setScope(scope.getId().toString());
        groupK1.setFrom(testFromDate);
        groupK1.setTo(testToDate);

        ParPartyNameVO partyNameK1 = new ParPartyNameVO();
        partyNameK1.setDisplayName("K1");
        partyNameK1.setMainPart("K1");
        partyNameK1.setPrefferedName(true);

        partyNameK1.setNameFormType(typePrimaryName);

        groupK1.setPartyNames(Collections.singletonList(partyNameK1));

        groupK1 = (ParPartyGroupVO) insertParty(groupK1);


        /** Vytvoření rodu **/
        ParDynastyVO dynastyR1 = new ParDynastyVO();
        RegRecordVO recordR1 = new RegRecordVO();

        recordR1.setRegisterTypeId(findRegisterTypeAddable(recordTypesForPartyType(typeR1.getPartyTypeId())).getId());
        recordR1.setScopeId(scope.getId());

        dynastyR1.setRecord(recordR1);
        dynastyR1.setGenealogy("R1");
        dynastyR1.setPartyType(typeR1);
        dynastyR1.setFrom(testFromDate);
        dynastyR1.setTo(testToDate);

        ParPartyNameVO partyNameR1 = new ParPartyNameVO();
        partyNameR1.setDisplayName("R1");
        partyNameR1.setMainPart("R1");
        partyNameR1.setPrefferedName(true);

        partyNameR1.setNameFormType(typePrimaryName);

        dynastyR1.setPartyNames(Collections.singletonList(partyNameR1));

        dynastyR1 = (ParDynastyVO) insertParty(dynastyR1);

        /** vytvoření události **/
        ParEventVO eventU1 = new ParEventVO();
        RegRecordVO recordU1 = new RegRecordVO();

        recordU1.setRegisterTypeId(findRegisterTypeAddable(recordTypesForPartyType(typeU1.getPartyTypeId())).getId());
        recordU1.setScopeId(scope.getId());

        eventU1.setRecord(recordU1);
        eventU1.setPartyType(typeU1);
        eventU1.setFrom(testFromDate);
        eventU1.setTo(testToDate);

        ParPartyNameVO partyNameU1 = new ParPartyNameVO();
        partyNameU1.setDisplayName("U1");
        partyNameU1.setMainPart("U1");
        partyNameU1.setPrefferedName(true);

        partyNameU1.setNameFormType(typePrimaryName);

        eventU1.setPartyNames(Collections.singletonList(partyNameU1));

        eventU1 = (ParEventVO) insertParty(eventU1);

        /** Filtrování + ověření **/
        List<ParPartyVO> parties = findParty(null, null, null, null, null);
        Assert.assertTrue("Očekáváme 4 záznamy", parties.size() == 4);

        for (ParPartyVO party : parties) {
            ParPartyNameVO name = party.getPartyNames().iterator().next();
            Assert.assertTrue("Očekáváme 1 preferované jméno", party.getPartyNames().size() == 1 && name.isPrefferedName());
            Assert.assertTrue("Očekáváme neprázdný rejstříkový záznam", party.getRecord() != null);
        }

        parties = findParty(null, null, null, groupK1.getPartyType().getPartyTypeId(), null);
        Assert.assertTrue("Očekáváme 1 záznam", parties.size() == 1);
        parties = findParty("U1", null, null, null, null);
        Assert.assertTrue("Očekáváme 1 záznam", parties.size() == 1);
        parties = findParty("O1", null, null, personO1.getPartyType().getPartyTypeId(), null);
        Assert.assertTrue("Očekáváme 1 záznam", parties.size() == 1);

        /** Změna jména a přidání doplňků */
        ParPartyNameVO newPersonName = new ParPartyNameVO();
        newPersonName.setDisplayName("ABCD");
        newPersonName.setMainPart("ABCD");
        newPersonName.setNameFormType(typePrimaryName);
        newPersonName.setNote("Poznámka jména");
        newPersonName.setValidFrom(testFromDate);
        newPersonName.setValidTo(testToDate);
        ParPartyNameComplementVO complement1 = new ParPartyNameComplementVO();
        ParPartyNameComplementVO complement2 = new ParPartyNameComplementVO();
        complement1.setComplement("IV");
        complement1.setComplementTypeId(findComplementTypeByCode(typeO1.getComplementTypes(), "3").getComplementTypeId());
        complement2.setComplement("rozpis");
        complement2.setComplementTypeId(findComplementTypeByCode(typeO1.getComplementTypes(), "1").getComplementTypeId());
        newPersonName.setPartyNameComplements(Arrays.asList(complement1, complement2));
        personO1.setPartyNames(Arrays.asList(partyNameO1, newPersonName));
        personO1 = (ParPersonVO) updateParty(personO1);

        Assert.assertTrue("Očekáváme 2 doplňková jména", personO1.getPartyNames().get(1).getPartyNameComplements().size() == 2);

        /** Změna preferovaného jména **/
        partyNameO1.setPrefferedName(false);
        newPersonName.setPrefferedName(true);
        personO1.setPartyNames(Arrays.asList(partyNameO1, newPersonName));
        personO1 = (ParPersonVO) updateParty(personO1);

        /** Změna attributů osoby **/
        String changedCharacter = "CharacterChange";
        String changedHistory = "HistoryChange";
        String changedSource = "SourceChange";
        personO1.setCharacteristics(changedCharacter);
        personO1.setHistory(changedHistory);
        personO1.setSourceInformation(changedSource);

        personO1 = (ParPersonVO) updateParty(personO1);
        personO1 = (ParPersonVO) getParty(personO1.getPartyId());

        Assert.assertTrue("Očekáváme projevené úpravy",
                personO1.getCharacteristics().equals(changedCharacter) &&
                        personO1.getHistory().equals(changedHistory) &&
                        personO1.getSourceInformation().equals(changedSource));

        /** Změna attributů rodu **/
        dynastyR1.setGenealogy("GenealogyChange");

        /** Změna creators rodu **/
        ParDynastyVO referenceDynastyR1 = new ParDynastyVO();
        referenceDynastyR1.setPartyId(dynastyR1.getPartyId());
        dynastyR1.setCreators(Arrays.asList(referenceDynastyR1, personO1));
        dynastyR1 = (ParDynastyVO) updateParty(dynastyR1);

        Assert.assertTrue("Očekáváme 2 creators", dynastyR1.getCreators().size() == 2);

        /** Změna creators rodu **/
        dynastyR1.setCreators(Collections.singletonList(personO1));
        dynastyR1 = (ParDynastyVO) updateParty(dynastyR1);
        dynastyR1 = (ParDynastyVO) getParty(dynastyR1.getPartyId());

        Assert.assertTrue("Očekáváme 1 creator", dynastyR1.getCreators().size() == 1);

        /** Změna attributů korporace **/
        String changedNorm = "NormChange";
        String changedOrganization = "OrganizationChange";
        String changedScopeNorm = "ScopeNormChange";
        String changedScope = "ScopeChange";
        groupK1.setFoundingNorm(changedNorm);
        groupK1.setOrganization(changedOrganization);
        groupK1.setScopeNorm(changedScopeNorm);
        groupK1.setScope(changedScope);

        groupK1 = (ParPartyGroupVO) updateParty(groupK1);
        groupK1 = (ParPartyGroupVO) getParty(groupK1.getPartyId());

        Assert.assertTrue("Očekáváme projevené úpravy",
                groupK1.getFoundingNorm().equals(changedNorm) &&
                        groupK1.getOrganization().equals(changedOrganization) &&
                        groupK1.getScopeNorm().equals(changedScopeNorm) &&
                        groupK1.getScope().equals(changedScope));

        /** Přidání identifikátorů korporace **/
        ParPartyGroupIdentifierVO identifier1 = new ParPartyGroupIdentifierVO();
        identifier1.setPartyId(groupK1.getPartyId());
        identifier1.setIdentifier("Identifier1");
        identifier1.setSource("Identifier2Source");

        ParPartyGroupIdentifierVO identifier2 = new ParPartyGroupIdentifierVO();
        identifier2.setPartyId(groupK1.getPartyId());
        identifier2.setIdentifier("Identifier2");
        identifier2.setSource("Identifier2Source");

        groupK1.setPartyGroupIdentifiers(Arrays.asList(identifier1, identifier2));
        groupK1 = (ParPartyGroupVO) updateParty(groupK1);

        Assert.assertTrue("Očekáváme 2 identifikátory", groupK1.getPartyGroupIdentifiers().size() == 2);

        /** Odebrání identifikátoru korporace **/
        groupK1.setPartyGroupIdentifiers(Collections.singletonList(identifier2));
        groupK1 = (ParPartyGroupVO) updateParty(groupK1);

        Assert.assertTrue("Očekáváme 1 identifikátor", groupK1.getPartyGroupIdentifiers().size() == 1);


        /** Test zda neexistuje nějaké heslo s tímto typem pro přidání (test metody findRecordForRelation) **/
        ParRelationTypeVO spawnRelationType = findRelationTypeByCode(typeO1.getRelationTypes(), "2");
        ParRelationRoleTypeVO spawnRelationRoleType = findRelationRoleTypeByCode(spawnRelationType.getRelationRoleTypes(), "11");
        List<RegRecordVO> recordForRelation = findRecordForRelation(null, null, null, spawnRelationRoleType.getRoleTypeId(), personO1.getPartyId());

        Assert.assertTrue("Očekáváme 0 záznamů", recordForRelation.size() == 0);

        /** Přidání relací osoby **/
        RegRecordVO record = new RegRecordVO();

        record.setScopeId(scope.getId());
        record.setCharacteristics("Characteristic");
        record.setRecord("GEO1");
        record.setRegisterTypeId(findRegisterTypeByCode(getRecordTypes(), "GEO_SPACE").getId());

        record = createRecord(record);

        /** Test zda existuje heslo s tímto typem pro přidání (test metody findRecordForRelation) **/
        recordForRelation = findRecordForRelation(null, null, null, spawnRelationRoleType.getRoleTypeId(), personO1.getPartyId());
        Assert.assertTrue("Očekáváme 1 záznam", recordForRelation.size() == 1);

        /** Vznik **/
        ParRelationVO spawnRelation = new ParRelationVO();
        ParRelationEntityVO spawnRelationEntity = new ParRelationEntityVO();
        spawnRelationEntity.setRecord(record);

        spawnRelationEntity.setRoleType(spawnRelationRoleType);
        spawnRelation.setRelationEntities(Collections.singletonList(spawnRelationEntity));
        spawnRelation.setComplementType(spawnRelationType);
        spawnRelation.setFrom(testFromDate);
        spawnRelation.setTo(testToDate);
        spawnRelation.setPartyId(personO1.getPartyId());
        spawnRelation = insertRelation(spawnRelation);

        /** Zánik **/
        ParRelationVO dieRelation = new ParRelationVO();
        ParRelationTypeVO dieRelationType = findRelationTypeByCode(typeO1.getRelationTypes(), "6");
        dieRelation.setComplementType(dieRelationType);
        dieRelation.setFrom(testFromDate);
        dieRelation.setTo(testToDate);
        dieRelation.setPartyId(personO1.getPartyId());
        dieRelation = insertRelation(dieRelation);

        /** Zaměstnavatel **/
        ParRelationVO workRelation = new ParRelationVO();
        ParRelationTypeVO workRelationType = findRelationTypeByCode(typeO1.getRelationTypes(), "17");
        ParRelationEntityVO workRelationEntity = new ParRelationEntityVO();
        workRelationEntity.setRecord(recordK1);
        workRelationEntity.setRoleType(findRelationRoleTypeByCode(workRelationType.getRelationRoleTypes(), "167"));
        workRelation.setComplementType(workRelationType);
        workRelation.setPartyId(personO1.getPartyId());
        workRelation = insertRelation(workRelation);

        /** Ověření **/
        personO1 = (ParPersonVO) getParty(personO1.getPartyId());
        Assert.assertTrue("Očekáváme 3 relace", personO1.getRelations().size() == 3);

        /** Úprava relací **/
        workRelation.setFrom(testFromDate);
        workRelation.setTo(testToDate);
        workRelation = updateRelation(workRelation);

        personO1 = (ParPersonVO) getParty(personO1.getPartyId());
        ParRelationVO testWorkRelation = personO1.getRelations().get(2);

        Assert.assertTrue("Očekáváme projevení změn",
                testWorkRelation.getFrom().getTextDate().equals(testFromDate.getTextDate()) &&
                        testWorkRelation.getTo().getTextDate().equals(testToDate.getTextDate()));

        /** Smazání relace **/
        deleteRelation(workRelation.getRelationId());

        personO1 = (ParPersonVO) getParty(personO1.getPartyId());
        Assert.assertTrue("Očekáváme 2 relace", personO1.getRelations().size() == 2);

        /** Vyhledání osob podle osoby **/
        List<ParPartyVO> partyList = findPartyForParty(personO1.getPartyId(), null, null, null, null, null);

        Assert.assertTrue("Očekáváme 4 záznamy", partyList.size() == 4);

        /** Smazání a kontrola počtu **/
        deleteParty(eventU1.getPartyId());
        parties = findParty(null, null, null, null, null);
        Assert.assertTrue("Očekáváme 3 záznamy", parties.size() == 3);
        partyList = findPartyForParty(personO1.getPartyId(), null, null, null, null, null);
        Assert.assertTrue("Očekáváme 3 záznamy", partyList.size() == 3);
    }


    private ParPartyTypeVO findPartyTypeByCode(final List<ParPartyTypeVO> list, final String code) {
        for (ParPartyTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    private RegRegisterTypeVO findRegisterTypeByCode(final List<RegRegisterTypeVO> list, final String code) {
        for (RegRegisterTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }

        for (RegRegisterTypeVO type : list) {
            if (type.getChildren() != null) {
                RegRegisterTypeVO res = findRegisterTypeByCode(type.getChildren(), code);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    private ParPartyNameFormTypeVO findPartyNameFormByCode(final List<ParPartyNameFormTypeVO> list, final String code) {
        for (ParPartyNameFormTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }


    private ParComplementTypeVO findComplementTypeByCode(final List<ParComplementTypeVO> list, final String code) {
        for (ParComplementTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }


    private ParRelationTypeVO findRelationTypeByCode(final List<ParRelationTypeVO> list, final String code) {
        for (ParRelationTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    private ParRelationRoleTypeVO findRelationRoleTypeByCode(final List<ParRelationRoleTypeVO> list, final String code) {
        for (ParRelationRoleTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    private ArrCalendarTypeVO findCalendarByCode(final List<ArrCalendarTypeVO> list, final String code) {
        for (ArrCalendarTypeVO item : list) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    private RegRegisterTypeVO findRegisterTypeAddable(List<RegRegisterTypeVO> list) {
        for (RegRegisterTypeVO type : list) {
            if (type.getAddRecord()) {
                return type;
            }
        }

        for (RegRegisterTypeVO type : list) {
            if (type.getChildren() != null) {
                RegRegisterTypeVO res = findRegisterTypeAddable(type.getChildren());
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }
}
