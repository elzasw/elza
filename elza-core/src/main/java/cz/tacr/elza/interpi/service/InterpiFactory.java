package cz.tacr.elza.interpi.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cz.tacr.elza.interpi.ws.wo.*;
import cz.tacr.elza.service.GroovyScriptService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.enums.InterpiClass;
import cz.tacr.elza.controller.vo.InterpiEntityMappingVO;
import cz.tacr.elza.controller.vo.InterpiRelationMappingVO;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.interpi.service.pqf.PQFQueryBuilder;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.service.vo.InterpiEntity;
import cz.tacr.elza.interpi.service.vo.MappingVO;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.PartyType;

/**
 * Třída pro konverzi objektů z a do INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 6. 12. 2016
 */
@Service
public class InterpiFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private InterpiSessionHolder interpiSessionHolder;

    @Autowired
    private PartyService partyService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private InterpiClient client;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private RegVariantRecordRepository variantRecordRepository;

    @Autowired
    private GroovyScriptService groovyScriptService;

    /**
     * Import rejstříkového hesla.
     *
     * @param entitaTyp INTERPI objekt
     * @param originalRecord původní rejstřík, může být null
     * @param interpiRecordId id INTERPI
     * @param regScope třída rejstříku
     * @param regExternalSystem externí systém
     *
     * @return uložené rejstříkové heslo
     */
    public RegRecord importRecord(final EntitaTyp entitaTyp, final RegRecord originalRecord,
            final String interpiRecordId,  final RegScope regScope, final RegExternalSystem regExternalSystem) {
        RegRecord regRecord = createRecord(entitaTyp, interpiRecordId, regExternalSystem, regScope, true);
        if (originalRecord != null) {
            regRecord.setRecordId(originalRecord.getRecordId());
            regRecord.setVersion(originalRecord.getVersion());
            regRecord.setUuid(originalRecord.getUuid());

            //smazání variantních hesel
            List<RegVariantRecord> oldVariants = variantRecordRepository.findByRegRecordId(originalRecord.getRecordId());
            variantRecordRepository.delete(oldVariants);
        }

        List<RegVariantRecord> variantRecordList = regRecord.getVariantRecordList();
        regRecord = registryService.saveRecord(regRecord, false);
        for (RegVariantRecord variantRecord : variantRecordList) {
            registryService.saveVariantRecord(variantRecord);
        }

        return regRecord;
    }

    /**
     * Import osoby.
     *
     * @param originalRecord původní rejstřík, může být null
     * @param interpiRecordId id INTERPI
     * @param isOriginator příznak zda je osoba původce
     * @param regScope třída rejstříku
     * @param regExternalSystem externí systém
     *
     * @return uložené rejstříkové heslo osoby
     */
    public RegRecord importParty(final InterpiEntity interpiEntity, final RegRecord originalRecord,
            final String interpiRecordId, final boolean isOriginator, final RegScope regScope,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        RegRecord regRecord = createPartyRecord(interpiEntity, interpiRecordId, regExternalSystem, regScope);

        Integer partyId = null;
        Integer partyVersion = null;
        if (originalRecord != null) {
            regRecord.setRecordId(originalRecord.getRecordId());
            regRecord.setVersion(originalRecord.getVersion());

            ParParty originalParty = partyService.findParPartyByRecord(originalRecord);

            List<ParRelation> relations = new ArrayList<>(originalParty.getRelations());
            if (CollectionUtils.isNotEmpty(relations)) {
                for (ParRelation relation : relations) {
                    partyService.deleteRelationAndSync(relation);
                }
            }
            originalParty.setRelations(null);
            partyService.saveParty(originalParty);

            partyId = originalParty.getPartyId();
            partyVersion = originalParty.getVersion();
        }

        ParParty newParty = createParty(regRecord, interpiEntity, isOriginator, regExternalSystem, mappings);
        newParty.setPartyId(partyId);
        newParty.setVersion(partyVersion);

        List<ParRelation> relations = newParty.getRelations();
        newParty.setRelations(null);
        ParParty parParty = partyService.saveParty(newParty);

        if (CollectionUtils.isNotEmpty(relations)) {
            for (ParRelation relation : relations) {
                List<ParRelationEntity> relationEntities = relation.getRelationEntities();
                relation.setRelationEntities(null);
                partyService.saveRelation(relation, relationEntities);
            }
        }

        return parParty.getRecord();
    }

    public String createSearchQuery(final List<ConditionVO> conditions, final boolean isParty) {
        return new PQFQueryBuilder(conditions).
                extend().
                party(isParty).
                createQuery();
    }

    public String getInterpiRecordId(final InterpiEntity interpiEntity) {
        List<IdentifikaceTyp> identifikace = interpiEntity.getIdentifikace();
        String interpiRecordId = getInterpiIdentifier(identifikace);

        if (interpiRecordId == null) {
            throw new SystemException("Záznam v INTERPI neexistuje", BaseCode.ID_NOT_EXIST);
        }
        return interpiRecordId;
    }

    private String getInterpiIdentifier(final List<IdentifikaceTyp> identifikace) {
        String interpiRecordId = null;
        for (IdentifikaceTyp identifikaceTyp : identifikace) {
            for (IdentifikatorTyp identifikator : identifikaceTyp.getIdentifikator()) {
                if (identifikator.getTyp().equals(IdentifikatorTypA.INTERPI)) { // chceme jen identifikátory typu INTERPI
                    interpiRecordId = identifikator.getValue();
                    break;
                }
            }

            if (interpiRecordId != null) {
                break;
            }
        }

        return interpiRecordId;
    }

    private String getInterpiSouvIdentifier(final List<IdentifikatorSouvTyp> identifikace) {
        String interpiRecordId = null;
        for (IdentifikatorSouvTyp identifikaceTyp : identifikace) {
            if (identifikaceTyp.getTyp() == IdentifikatorSouvTypA.INTERPI) { // chceme jen identifikátory typu INTERPI
                interpiRecordId = identifikaceTyp.getValue();
                break;
            }

            if (interpiRecordId != null) {
                break;
            }
        }

        if (interpiRecordId == null) {
            throw new SystemException("Záznam v INTERPI neexistuje", BaseCode.ID_NOT_EXIST);
        }

        return interpiRecordId;
    }

    /**
     * Vytvoří rejstříkové heslo.
     *
     * @param entitaTyp záznam z INTERPI
     * @param interpiPartyId externí id osoby
     * @param regExternalSystem systém ze kterého je osoba
     * @param regScope třída rejstříků do které se importuje
     * @param generateVariantNames příznak zda se mají generovat variantní jména
     *
     * @return rejstříkové heslo
     */
    private RegRecord createRecord(final EntitaTyp entitaTyp, final String interpiPartyId,
            final RegExternalSystem regExternalSystem, final RegScope regScope, final boolean generateVariantNames) {
        InterpiEntity interpiEntity = new InterpiEntity(entitaTyp);
        RegRecord regRecord = createPartyRecord(interpiEntity, interpiPartyId, regExternalSystem, regScope);

        ExternalRecordVO recordVO = groovyScriptService.convertListToExternalRecordVO(Collections.singletonList(entitaTyp), generateVariantNames, this).
                iterator().next();

        List<String> strucnaCharakteristika = interpiEntity.getPopisTyp().stream().filter(i -> PopisTypA.STRUČNÁ_CHARAKTERISTIKA.equals(i.getTyp())).map(PopisTyp::getTextPopisu).collect(Collectors.toList());
        regRecord.setCharacteristics(String.join(", ", strucnaCharakteristika));
        regRecord.setRecord(recordVO.getName());

        List<RegVariantRecord> regVariantRecords = new ArrayList<>(recordVO.getVariantNames().size());
        for (String variantName : recordVO.getVariantNames()) {
            RegVariantRecord regVariantRecord = new RegVariantRecord();
            regVariantRecord.setRecord(variantName);
            regVariantRecord.setRegRecord(regRecord);

            regVariantRecords.add(regVariantRecord);
        }
        regRecord.setVariantRecordList(regVariantRecords);

        return regRecord;
    }

    /**
     * Vytvoří rejstříkové heslo se základními informacemi potřebnými pro uložení osoby.
     *
     * @param interpiEntity mapa hodnot z interpi
     * @param interpiPartyId extrní id osoby
     * @param regExternalSystem systém ze kterého je osoba
     * @param regScope třída rejstříků do které se importuje
     *
     * @return rejstříkové heslo
     */
    private RegRecord createPartyRecord(final InterpiEntity interpiEntity,
            final String interpiPartyId, final RegExternalSystem regExternalSystem, final RegScope regScope) {
        RegRecord regRecord = new RegRecord();

        regRecord.setExternalId(interpiPartyId);
        regRecord.setExternalSystem(regExternalSystem);

        String note = getNote(interpiEntity);
        regRecord.setNote(note);

        RegRegisterType regRegisterType = getRegisterType(interpiEntity);
        if (BooleanUtils.isFalse(regRegisterType.getAddRecord())) {
            throw new IllegalStateException("Do typu rejstříku s kódem " + regRegisterType.getCode() + " nelze přidávat záznamy.");
        }
        regRecord.setRegisterType(regRegisterType);

        regRecord.setScope(regScope);

        return regRecord;
    }

    /**
     * Vytvoří poznámku.
     *
     * @param interpiEntity mapa hodnot z interpi
     *
     * @return poznámka, může být null
     */
    private String getNote(final InterpiEntity interpiEntity) {
        List<String> notes = new LinkedList<>();
        List<PopisTyp> popisTypList = interpiEntity.getPopisTyp();
        for (PopisTyp popisTyp : popisTypList) {
            switch (popisTyp.getTyp()) {
                case POPIS:
                    String popis = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (popis != null) {
                        notes.add(popis);
                    }
                    break;
                }
        }

        String note = null;
        if (!notes.isEmpty()) {
            note = StringUtils.join(notes, ", ");
        }
        return note;
    }

    private String getSourceInformation(final InterpiEntity interpiEntity) {
        List<ZdrojTyp> zdrojTypList = interpiEntity.getZdrojTyp();
        List<String> sourceInformations = new ArrayList<>(zdrojTypList.size());
        for (ZdrojTyp zdrojTyp : zdrojTypList) {
            String standardizovanyPopis = StringUtils.trimToNull(zdrojTyp.getStandardizovanyPopis());
            if (standardizovanyPopis != null) {
                sourceInformations.add(standardizovanyPopis);
            }
        }

        String sourceInformation = null;
        if (!sourceInformations.isEmpty()) {
            sourceInformation = StringUtils.join(sourceInformations, ", ");
        }
        return sourceInformation;
    }

    /**
     * Zjistí typ rejstříku.
     *
     * @param interpiEntity hodnoty entity
     *
     * @return typ rejstříku
     */
    public RegRegisterType getRegisterType(final InterpiEntity interpiEntity) {
        String registryTypeName;
        PodtridaTyp podTrida = interpiEntity.getPodTrida();
        if (podTrida == null) {
            TridaTyp trida = interpiEntity.getTrida();
            registryTypeName = trida.value();
        } else {
            registryTypeName = podTrida.value();
        }

        String registryTypeCode = getRegistryTypeCode(registryTypeName);
        RegRegisterType regRegisterType = registerTypeRepository.findRegisterTypeByCode(registryTypeCode);
        if (regRegisterType == null) {
            throw new ObjectNotFoundException("Typ jména " + registryTypeName + " neexistuje", RegistryCode.REGISTRY_TYPE_NOT_FOUND).set("name", registryTypeName);
        }

        return regRegisterType;
    }

    private String getRegistryTypeCode(final String registryTypeName) {
        Map<String, String> registryCodeMap = new HashMap<>();
        registryCodeMap.put("administrativně vymezená území s vlastní správou", "REGION");
        registryCodeMap.put("orgány, organizační složky a příspěvkové organizace veřejné správy", "PUBLIC_ADMINISTRATION");
        registryCodeMap.put("sdružení organizací", "ORGANIZATION");
        registryCodeMap.put("vojenské a bezpečnostní jednotky", "ARMY");
        registryCodeMap.put("organizace založené za účelem podnikání", "COMPANY");
        registryCodeMap.put("politické organizace", "POLITICAL_PARTY");
        registryCodeMap.put("náboženské organizace a instituce", "CHURCH");
        registryCodeMap.put("kulturní, výchovné, výzkumné a zdravotnické organizace a instituce", "HEALTH_AND_EDU");
        registryCodeMap.put("nadace a nadační fondy", "CHARITY");
        registryCodeMap.put("profesní a zájmové organizace", "GUILD");
        registryCodeMap.put("spolky, společenské organizace", "CLUB");
        registryCodeMap.put("pojmenované části korporací", "BRANCH");
        registryCodeMap.put("fyzická osoba", "PERSON_INDIVIDUAL");
        registryCodeMap.put("fiktivní fyzická osoba", "FICTIVE_INDIVIDUAL");
        registryCodeMap.put("bytost", "PERSON_BEING");
        registryCodeMap.put("zvíře", "PERSON_ANIMAL");
        registryCodeMap.put("rodina, rod", "FAMILY");
        registryCodeMap.put("větev rodu", "FAMILY_BRANCH");
        registryCodeMap.put("fiktivní rod", "FICTIVE_DYNASTY");
        registryCodeMap.put("administrativně či jinak lidmi vymezená území", "GEO_UNIT");
        registryCodeMap.put("administrativně vymezené části přírody", "GEO_NATURE_RES");
        registryCodeMap.put("vymezené obvody a správní celky", "GEO_ADMIN_UNIT");
        registryCodeMap.put("geomorfologické útvary na dně moře", "GEO_SEA_FORMATION");
        registryCodeMap.put("geomorfologické útvary na zemském povrchu", "GEO_FORMATION");
        registryCodeMap.put("vodstvo a vodní plocha, vodní tok", "GEO_WATERS");
        registryCodeMap.put("pojmenované útvary", "GEO_SHAPES");
        registryCodeMap.put("pojmenované trvalé klimatické jevy", "GEO_CLIMATIC_PHEN");
        registryCodeMap.put("Vesmír a jeho části", "GEO_SPACE");
        // Default mapping for geo objects
        registryCodeMap.put("geografický objekt", "GEO_NATURE_RES");
        registryCodeMap.put("organizované akce a události", "EVENT_EVENT");
        registryCodeMap.put("ozbrojené střety (bitvy, války, obléhání...)", "EVENT_MILITARY");
        registryCodeMap.put("lidové zvyky, významné dny a svátky", "EVENT_TRADITION");
        registryCodeMap.put("dočasné přírodní jevy", "EVENT_NATURE");
        registryCodeMap.put("autorská a umělecká díla", "ARTWORK_ARTWORK");
        registryCodeMap.put("všeobecně známé dokumenty, smlouvy, zákony, předpisy, normy", "ARTWORK_CHARTER");
        registryCodeMap.put("vyznamenání, ceny, soutěžní trofeje", "ARTWORK_AWARD");
        registryCodeMap.put("výrobky a jejich typová označení, obchodní značky, odrůdy, plemena vytvořená člověkem", "ARTWORK_PROD");
        registryCodeMap.put("názvy společenských, dětských her", "ARTWORK_");
        registryCodeMap.put("programy, projekty, granty", "ARTWORK_PROJ");
        registryCodeMap.put("stavby, trasy, zásahy do přírodních útvarů s vlastním jménem nebo jinou identifikací", "ARTWORK_CONSTR");
        registryCodeMap.put("nepojmenované objekty a jejich fyzické části", "TERM_GENERAL");
        registryCodeMap.put("kategorie a skupiny nepojmenovaných osob", "TERM_PERSON");
        registryCodeMap.put("kategorie a skupiny nepojmenovaných korporací", "TERM_CORP");
        registryCodeMap.put("materiály a techniky", "TERM_MATER");
        registryCodeMap.put("formy a žánry", "TERM_GENRE");
        registryCodeMap.put("systematická nomenklatura", "TERM_NOMENC");
        registryCodeMap.put("abstraktní entity", "TERM_ABSTRACT");
        // obecny pojem - jen trida
        registryCodeMap.put("obecný pojem", "TERM_GENERAL");
        registryCodeMap.put("rod/rodina", "FAMILY");

        String registryTypeCode = registryCodeMap.get(registryTypeName);

        if (StringUtils.isBlank(registryTypeCode)) {
            throw new IllegalStateException("Chybí mapování pro typ rejstříku " + registryTypeName);
        }

        return registryTypeCode;
    }

    /**
     * Naplní předanou osobu.
     *
     * @param parParty osoba
     * @param interpiEntity informace z INTERPI
     * @param regExternalSystem externí systém
     * @param mappings mapování vztahů
     */
    private void fillParty(final ParParty parParty, final InterpiEntity interpiEntity,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
//        parParty.setPartyCreators(null); // po dohodě s Honzou Vejskalem neimportovat, není jak

        List<ParPartyName> partyNames = new LinkedList<>();
        List<OznaceniTyp> variantniOznaceniList = interpiEntity.getVariantniOznaceni();
        if (CollectionUtils.isNotEmpty(variantniOznaceniList)) {
            for (OznaceniTyp variantniOznaceni : variantniOznaceniList) {
                ParPartyName parPartyName = createPartyName(interpiEntity, variantniOznaceni, parParty, false);
                if (parPartyName != null) {
                    partyNames.add(parPartyName);
                }
            }
        }
        parParty.setPartyNames(partyNames);

        OznaceniTyp preferovaneOznaceni = interpiEntity.getPreferovaneOznaceni();
        ParPartyName preferredName = createPartyName(interpiEntity, preferovaneOznaceni, parParty, true);
        parParty.setPreferredName(preferredName);
        partyNames.add(preferredName);

        String sourceInformation = getSourceInformation(interpiEntity);
        parParty.setSourceInformation(sourceInformation);

        fillAdditionalInfo(parParty, interpiEntity);

        if (parParty.isOriginator() && CollectionUtils.isNotEmpty(mappings)) {
            fillRelations(parParty, interpiEntity, regExternalSystem, mappings);
        }
    }

    private void fillRelations(final ParParty parParty, final InterpiEntity interpiEntity,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        List<UdalostTyp> pocatekExistence = interpiEntity.getPocatekExistence();
        List<UdalostTyp> konecExistence = interpiEntity.getKonecExistence();
        List<UdalostTyp> udalostList = interpiEntity.getUdalost();
        List<UdalostTyp> zmenaList = interpiEntity.getZmena();
        List<SouvisejiciTyp> souvisejiciEntitaList = interpiEntity.getSouvisejiciEntita();

        List<ParRelation> relations = new LinkedList<>();
        List<ParRelation> createRelations = createRelations(pocatekExistence, parParty, InterpiClass.POCATEK_EXISTENCE,
                regExternalSystem, mappings);
        if (CollectionUtils.isNotEmpty(createRelations)) {
            relations.addAll(createRelations);
        }
        List<ParRelation> endRelations = createRelations(konecExistence, parParty, InterpiClass.KONEC_EXISTENCE,
                regExternalSystem, mappings);
        if (CollectionUtils.isNotEmpty(endRelations)) {
            relations.addAll(endRelations);
        }
        List<ParRelation> relRelations = createRelations(udalostList, parParty, InterpiClass.UDALOST,
                regExternalSystem, mappings);
        if (CollectionUtils.isNotEmpty(relRelations)) {
            relations.addAll(relRelations);
        }
        List<ParRelation> changeRelations = createRelations(zmenaList, parParty, InterpiClass.ZMENA,
                regExternalSystem, mappings);
        if (CollectionUtils.isNotEmpty(changeRelations)) {
            relations.addAll(changeRelations);
        }
        List<ParRelation> entityRelations = createEntityRelations(souvisejiciEntitaList, parParty, InterpiClass.SOUVISEJICI_ENTITA,
                regExternalSystem, mappings);
        if (CollectionUtils.isNotEmpty(entityRelations)) {
            relations.addAll(entityRelations);
        }


        parParty.setRelations(relations);
    }

    private List<ParRelation> createEntityRelations(final List<SouvisejiciTyp> souvisejiciEntitaList,
            final ParParty parParty, final InterpiClass interpiClass,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        if (CollectionUtils.isEmpty(souvisejiciEntitaList)) {
            return Collections.emptyList();
        }

        Map<String, ParRelation> relationsMap = new HashMap<>();
        for (SouvisejiciTyp souvisejiciTyp : souvisejiciEntitaList) {
            String interpiRoleType = getInterpiRoleType(souvisejiciTyp);
            String interpiId = getInterpiSouvIdentifier(souvisejiciTyp.getIdentifikator());
            MappingVO mappingVO = findRelationMapping(mappings, interpiClass, null, interpiRoleType, interpiId);
            if (mappingVO == null || !mappingVO.isImportRelation()) { // přeskočení
                continue;
            }

            String relationCode = mappingVO.getParRelationType().getCode();
            ParRelation entityRelation = relationsMap.get(relationCode);
            if (entityRelation == null) {
                entityRelation = createParRelation(parParty, null, mappingVO.getParRelationType());
                relationsMap.put(entityRelation.getRelationType().getCode(), entityRelation);
            }

            createParRelationEntity(parParty, regExternalSystem, entityRelation, souvisejiciTyp,
                    mappingVO.getParRelationRoleType());
        }

        return new ArrayList<>(relationsMap.values());
    }

    private List<ParRelation> createRelations(final List<UdalostTyp> udalostList, final ParParty parParty, final InterpiClass interpiClass,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        List<ParRelation> relations = new LinkedList<>();
        if (CollectionUtils.isEmpty(udalostList)) {
            return relations;
        }

        for (UdalostTyp udalostTyp : udalostList) {
            List<SouvisejiciTyp> souvisejiciEntitaList = udalostTyp.getSouvisejiciEntita();

            UdalostTypA udalostTypA = udalostTyp.getTyp();
            if (udalostTypA == null) {
                throw new IllegalStateException("Vztah nemá typ.");
            }
            String interpiRelationType = udalostTypA.value();

            if (CollectionUtils.isEmpty(souvisejiciEntitaList)  ) {
                MappingVO mappingVO = findRelationMapping(mappings, interpiClass, interpiRelationType, null, null);
                if (mappingVO == null || !mappingVO.isImportRelation()) { // přeskočení
                    continue;
                }

                ParRelation parRelation = createParRelation(parParty, udalostTyp, mappingVO.getParRelationType());
                relations.add(parRelation);
            } else {
                ParRelation parRelation = null;
                for (SouvisejiciTyp souvisejiciTyp : souvisejiciEntitaList) {
                    String interpiRoleType = getInterpiRoleType(souvisejiciTyp);
                    String interpiId = getInterpiSouvIdentifier(souvisejiciTyp.getIdentifikator());
                    MappingVO mappingVO = findRelationMapping(mappings, interpiClass, interpiRelationType, interpiRoleType, interpiId);
                    if (mappingVO == null || !mappingVO.isImportRelation()) { // přeskočení
                        continue;
                    }

                    if (parRelation == null) {
                        parRelation = createParRelation(parParty, udalostTyp, mappingVO.getParRelationType());
                        relations.add(parRelation);
                    }

                    createParRelationEntity(parParty, regExternalSystem, parRelation, souvisejiciTyp, mappingVO.getParRelationRoleType());
                }
            }
        }

        return relations;
    }

    private MappingVO findRelationMapping(final List<MappingVO> mappings, final InterpiClass interpiClass,
            final String interpiRelationType, final String interpiRoleType, final String entityId) {
        Assert.isTrue(interpiRelationType != null || interpiRoleType != null, "Podmínka musí platit");

        if (mappings == null) {
            return null;
        }

        Predicate<MappingVO> classCondition = (m -> interpiClass == m.getInterpiClass()); // podmínka na třídu

        Predicate<MappingVO> typeCondition = null;
        if (interpiRelationType == null) {
            typeCondition = (m -> m.getInterpiRelationType() == null); // entita bez vztahu
        } else {
            typeCondition = (m -> interpiRelationType.equalsIgnoreCase(m.getInterpiRelationType())); // vztah
        }

        Predicate<MappingVO> roleCondition = null;
        if (interpiRoleType == null) {
            roleCondition = (m -> m.getInterpiRoleType() == null); // vztah bez entity
        } else {
            roleCondition = (m -> interpiRoleType.equalsIgnoreCase(m.getInterpiRoleType())); // entita
        }

        Predicate<MappingVO> entityIdCondition;
        if (entityId == null) {
            entityIdCondition = (m -> true);
        } else {
            entityIdCondition = (m -> entityId.equals(m.getInterpiId()));
        }

        return mappings.stream().
                filter(classCondition).
                filter(typeCondition).
                filter(roleCondition).
                filter(entityIdCondition).
                findFirst().
                orElse(null);
    }

    private void createParRelationEntity(final ParParty parParty, final RegExternalSystem regExternalSystem,
            final ParRelation parRelation, final SouvisejiciTyp souvisejiciTyp, final ParRelationRoleType parRelationRoleType) {
        ParRelationEntity parRelationEntity = new ParRelationEntity();
        parRelationEntity.setNote(souvisejiciTyp.getPoznamka());
        parRelationEntity.setRelation(parRelation);

        RegRecord entityRecord = getRelationEntityRecord(parParty, regExternalSystem, souvisejiciTyp);
        parRelationEntity.setRecord(entityRecord);

        parRelationEntity.setRoleType(parRelationRoleType);

        parRelation.getRelationEntities().add(parRelationEntity);
    }

    /**
     * Získání rejstříkového hesla pro entitu ve vztahu.
     *
     * @param parParty osoba
     * @param regExternalSystem externí systém
     * @param souvisejiciTyp entita ve vztahu
     *
     * @return rejstříkové heslo entity
     */
    private RegRecord getRelationEntityRecord(final ParParty parParty, final RegExternalSystem regExternalSystem,
            final SouvisejiciTyp souvisejiciTyp) {
        String interpiId = getInterpiSouvIdentifier(souvisejiciTyp.getIdentifikator());
        RegRecord entityRecord = recordRepository.findRegRecordByExternalIdAndExternalSystemCodeAndScope(interpiId,
                regExternalSystem.getCode(), parParty.getRecord().getScope());

        if (entityRecord == null) { // pokud neexistiuje v db tak se importuje bez vztahů
            EntitaTyp entitaTyp = interpiSessionHolder.getInterpiEntitySession().getRelatedEntity(interpiId);
            if (entitaTyp == null) {
                entitaTyp = client.findOneRecord(interpiId, regExternalSystem);
            }
            InterpiEntity interpiEntity = new InterpiEntity(entitaTyp);
            if (isParty(interpiEntity)) {
                entityRecord = importParty(interpiEntity, null, interpiId, false, parParty.getRecord().getScope(), regExternalSystem, null);
            } else {
                entityRecord = importRecord(entitaTyp, null, interpiId, parParty.getRecord().getScope(), regExternalSystem);
            }
        }
        return entityRecord;
    }

    private ParRelation createParRelation(final ParParty parParty, final UdalostTyp udalostTyp, final ParRelationType parRelationType) {
        KomplexniDataceTyp dataceFrom = null;
        KomplexniDataceTyp dataceTo = null;
        KomplexniDataceTyp datace1 = null; // bez typu
        KomplexniDataceTyp datace2 = null; // bez typu

        if (udalostTyp != null) {
            List<KomplexniDataceTyp> dataceList = udalostTyp.getDatace(); // podle xsd mohou být maximálně 2
            if (CollectionUtils.isNotEmpty(dataceList)) {
                for (KomplexniDataceTyp dataceTyp : dataceList) {
                    if (dataceTyp.getTyp() == KomplexniDataceTypA.ZAČÁTEK) {
                        dataceFrom = dataceTyp;
                    } else if (dataceTyp.getTyp() == KomplexniDataceTypA.KONEC) {
                        dataceTo = dataceTyp;
                    } else {
                        if (datace1 == null) {
                            datace1 = dataceTyp;
                        } else {
                            datace2 = dataceTyp;
                        }
                    }
                }
            }
        }

        ParUnitdate unitdateFrom = null;
        ParUnitdate unitdateTo = null;
        if (dataceFrom != null) {
            unitdateFrom = convertDataceToParUnitdate(dataceFrom);
        } else if (datace1 != null) {
            unitdateFrom = convertDataceToParUnitdate(datace1);
        }

        if (dataceTo != null) {
            unitdateTo = convertDataceToParUnitdate(dataceTo);
        } else if (datace1 != null && dataceFrom != null) {
            unitdateTo = convertDataceToParUnitdate(datace1);
        } else if (datace2 != null) {
            unitdateTo = convertDataceToParUnitdate(datace2);
        }

        ParRelation parRelation = new ParRelation();
        parRelation.setParty(parParty);
        parRelation.setFrom(unitdateFrom);
        parRelation.setTo(unitdateTo);
        parRelation.setRelationEntities(new LinkedList<>());

        if (udalostTyp != null) {
            parRelation.setNote(udalostTyp.getPoznamka());
        }

        parRelation.setRelationType(parRelationType);

//            parRelation.setSource(source); // po dohodě s Honzou Vejskalem neimportovat
        return parRelation;
    }

    private ParUnitdate convertDataceToParUnitdate(final KomplexniDataceTyp dataceTyp) {
        ParUnitdate parUnitdate = new ParUnitdate();

        String dateFrom = convertDateFormat(dataceTyp.getDatumOd());
        String dateTo = convertDateFormat(dataceTyp.getDatumDo());

        if (StringUtils.isNotBlank(dateFrom) && StringUtils.isNotBlank(dateTo)) {
            if (dateFrom.equals(dateTo)) {
                UnitDateConvertor.convertToUnitDate(dateFrom, parUnitdate);
            } else {
                UnitDateConvertor.convertToUnitDate(dateFrom + "-" + dateTo, parUnitdate);
            }
        } else if (StringUtils.isNotBlank(dateFrom)) {
            UnitDateConvertor.convertToUnitDate(dateFrom, parUnitdate);
        } else if (StringUtils.isNotBlank(dateTo)) {
            UnitDateConvertor.convertToUnitDate(dateTo, parUnitdate);
        }

        parUnitdate.setNote(dataceTyp.getPoznamka());
        parUnitdate.setTextDate(dataceTyp.getTextDatace());

        return parUnitdate;
    }

    /** Převede datum z formátu yyyy-m-d na d.m.yyyy */
    private String convertDateFormat(final String date) {
        if (StringUtils.isBlank(date) || date.startsWith("-")) { // datum je prázdný nebo záporný
            return null;
        }

        String[] parts = date.replaceAll(" ", "").split("-");
        List<String> partsList = Arrays.asList(parts);
        Collections.reverse(partsList);

        return StringUtils.join(partsList, ".");
    }

    /**
     * Nastaví různé informace které jsou v INTERPI uloženy jako popis.
     */
    private void fillAdditionalInfo(final ParParty newParty, final InterpiEntity interpiEntity) {
        List<PopisTyp> popisTypList = interpiEntity.getPopisTyp();

        List<String> characteristicsList = new LinkedList<>();
        List<String> historyList = new LinkedList<>();
        List<String> cvList = new LinkedList<>();
        List<String> genealogyList = new LinkedList<>();
        List<String> foundingNormList = new LinkedList<>();
        List<String> scopeNormList = new LinkedList<>();
        List<String> organizationList = new LinkedList<>();
        List<String> scopeList = new LinkedList<>();
        for (PopisTyp popisTyp : popisTypList) {
            switch (popisTyp.getTyp()) {
                case FUNKCE:
                    String funkce = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (funkce != null) {
                        scopeList.add(funkce);
                    }
                    break;
                case GENEALOGIE:
                    String genealogie = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (genealogie != null) {
                        genealogyList.add(genealogie);
                    }
                    break;
                case HISTORIE:
                    String historie = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (historie != null) {
                        historyList.add(historie);
                    }
                    break;
                case ŽIVOTOPIS:
                    String zivotopis = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (zivotopis != null) {
                        cvList.add(zivotopis);
                    }
                    break;
                case NORMY_KONSTITUTIVNÍ:
                    String normyKonstitucni = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (normyKonstitucni != null) {
                        foundingNormList.add(normyKonstitucni);
                    }
                    break;
                case NORMY_PŮSOBNOST_PŮVODCE:
                    String normyPusobnostPuvodce = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (normyPusobnostPuvodce != null) {
                        scopeNormList.add(normyPusobnostPuvodce);
                    }
                    break;
                case STRUČNÁ_CHARAKTERISTIKA:
                    String strucnaCharakteristika = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (strucnaCharakteristika != null) {
                        characteristicsList.add(strucnaCharakteristika);
                    }
                    break;
                case VNITŘNÍ_STRUKTURA:
                    String vnitrniStruktura = StringUtils.trimToNull(popisTyp.getTextPopisu());
                    if (vnitrniStruktura != null) {
                        organizationList.add(vnitrniStruktura);
                    }
                    break;
            }
        }

        String characteristics = null;
        if (!characteristicsList.isEmpty()) {
            characteristics = StringUtils.join(characteristicsList, ", ");
        }
        newParty.setCharacteristics(characteristics);


        String history = null;
        if (!historyList.isEmpty()) {
            history = StringUtils.join(historyList, ", ");
        }

        String cv = null;
        if (!cvList.isEmpty()) {
            cv = StringUtils.join(cvList, ", ");
        }

        String historyAndCV;
        if (history == null && cv == null) {
            historyAndCV = null;
        } else if (history == null) {
            historyAndCV = cv;
        } else {
            historyAndCV = history + cv;
        }
        newParty.setHistory(historyAndCV);

        if (newParty instanceof ParDynasty) {
            ParDynasty parDynasty = (ParDynasty) newParty;

            String genealogy = null;
            if (!genealogyList.isEmpty()) {
                genealogy = StringUtils.join(genealogyList, ", ");
            }
            parDynasty.setGenealogy(genealogy);
        } else if (newParty instanceof ParPartyGroup) {
            ParPartyGroup parPartyGroup = (ParPartyGroup) newParty;

            String foundingNorm = null;
            if (!foundingNormList.isEmpty()) {
                foundingNorm = StringUtils.join(foundingNormList, ", ");
            }
            parPartyGroup.setFoundingNorm(foundingNorm);

            String organization = null;
            if (!organizationList.isEmpty()) {
                organization = StringUtils.join(organizationList, ", ");
            }
            parPartyGroup.setOrganization(organization);

            String scope = null;
            if (!scopeList.isEmpty()) {
                scope = StringUtils.join(scopeList, ", ");
            }
            parPartyGroup.setScope(scope);

            String scopeNorm = null;
            if (!scopeNormList.isEmpty()) {
                scopeNorm = StringUtils.join(scopeNormList, ", ");
            }
            parPartyGroup.setScopeNorm(scopeNorm);

            List<KodovaneTyp> kodovaneUdajeList = interpiEntity.getKodovaneUdaje();
            List<ParPartyGroupIdentifier> partyGroupIdentifiers = new ArrayList<>(kodovaneUdajeList.size());
            parPartyGroup.setPartyGroupIdentifiers(partyGroupIdentifiers);
            for (KodovaneTyp kodovaneTyp : kodovaneUdajeList) {
                ParPartyGroupIdentifier partyGroupIdentifier = new ParPartyGroupIdentifier();
                partyGroupIdentifiers.add(partyGroupIdentifier);

                String datace = kodovaneTyp.getDatace();
                if (StringUtils.isNotBlank(datace)) {
                    // po dohodě s Honzou Vejskalem se to bude zatím nastavovat takto
                    ParUnitdate validFrom = new ParUnitdate();
                    validFrom.setTextDate(datace);
                    partyGroupIdentifier.setFrom(validFrom);
                }

                partyGroupIdentifier.setIdentifier(kodovaneTyp.getKod());
                partyGroupIdentifier.setNote(kodovaneTyp.getPoznámka());
                partyGroupIdentifier.setPartyGroup(parPartyGroup);

                KodovaneTypA kodovaneTypA = kodovaneTyp.getTyp();
                if (kodovaneTypA != null) {
                    partyGroupIdentifier.setSource(kodovaneTypA.value());
                }
//                partyGroupIdentifier.setTo(to);
            }
        }
    }

    private ParPartyName createPartyName(final InterpiEntity interpiEntity, final OznaceniTyp oznaceniTyp, final ParParty parParty, final boolean isPreferred) {
        ParPartyName partyName = new ParPartyName();

        ParPartyNameFormType parPartyNameFormType = null;
        OznaceniTypTypA typ = oznaceniTyp.getTyp();
        if (typ != null) {
            String partyNameFormTypeName = typ.value();
            parPartyNameFormType = partyNameFormTypeRepository.findByName(partyNameFormTypeName);
            if (parPartyNameFormType == null) {
                throw new ObjectNotFoundException("Nebyl nalezen typ formy jména podle " + partyNameFormTypeName, ArrangementCode.PARTY_NAME_FORM_TYPE_NOT_FOUND).set("name", partyNameFormTypeName);
            }
        }
        partyName.setNameFormType(parPartyNameFormType);

        if (isPreferred) {
            Set<String> degreesBefore = new HashSet<>();
            Set<String> degreesAfter = new HashSet<>();
            List<TitulTyp> titulTypList = interpiEntity.getTitul();
            for (TitulTyp titulTyp : titulTypList) {
                if (TitulTypA.TITULY_PŘED_JMÉNEM == titulTyp.getTyp()) {
                    degreesBefore.add(titulTyp.getValue());
                } else if (TitulTypA.TITULY_ZA_JMÉNEM == titulTyp.getTyp()) {
                    degreesAfter.add(titulTyp.getValue());
                }
            }
            partyName.setDegreeAfter(StringUtils.join(degreesAfter, " "));
            partyName.setDegreeBefore(StringUtils.join(degreesBefore, " "));
        }

        partyName.setMainPart(oznaceniTyp.getHlavniCast().getValue());
        partyName.setNote(oznaceniTyp.getPoznamka());


        VedlejsiCastTyp vedlejsiCast = oznaceniTyp.getVedlejsiCast();
        if (vedlejsiCast != null) {
            partyName.setOtherPart(vedlejsiCast.getValue());
        }
        partyName.setParty(parParty);

        List<ParPartyNameComplement> partyNameComplements = createPartyNameComplements(oznaceniTyp.getDoplnek(), partyName);
        partyName.setPartyNameComplements(partyNameComplements);

        String datace = oznaceniTyp.getDatace();
        if (StringUtils.isNotBlank(datace)) {
            // po dohodě s Honzou Vejskalem se to bude zatím nastavovat takto
            ParUnitdate validFrom = new ParUnitdate();
            validFrom.setTextDate(datace);
            partyName.setValidFrom(validFrom);
        }

        return partyName;
    }

    private List<ParPartyNameComplement> createPartyNameComplements(final List<DoplnekTyp> doplnekTypList, final ParPartyName partyName) {
        List<ParPartyNameComplement> parPartyNameComplements = new LinkedList<>();

        if (doplnekTypList != null) {
            for (DoplnekTyp doplnekTyp : doplnekTypList) {
                ParPartyNameComplement parPartyNameComplement = new ParPartyNameComplement();
                parPartyNameComplement.setComplement(doplnekTyp.getValue());

                String parPartyNameComplementName = doplnekTyp.getTyp().value();
                ParComplementType parComplementType = complementTypeRepository.findByName(parPartyNameComplementName);

                parPartyNameComplement.setComplementType(parComplementType);
                parPartyNameComplement.setPartyName(partyName);

                parPartyNameComplements.add(parPartyNameComplement);
            }
        }

        return parPartyNameComplements;
    }

    /**
     * Zjistí zda data entity odpovídají osobě.
     */
    public boolean isParty(final InterpiEntity interpiEntity) {
        TridaTyp trida = interpiEntity.getTrida();

        List<TridaTyp> partyTypes = Arrays.asList(TridaTyp.KORPORACE, TridaTyp.OSOBA_BYTOST, TridaTyp.ROD_RODINA, TridaTyp.UDÁLOST);

        return partyTypes.contains(trida);
    }

    /**
     * Vytvoří osobu.
     *
     * @param regRecord rejstříkové heslo osoby
     * @param isOriginator příznak původce
     * @param regExternalSystem systém ze kterého je osoba
     * @param mappings mapování vztahů
     *
     * @return osoba
     */
    private ParParty createParty(final RegRecord regRecord, final InterpiEntity interpiEntity,
            final boolean isOriginator, final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        TridaTyp trida = interpiEntity.getTrida();

        ParParty parParty;
        ParPartyType parPartyType;
        switch (trida) {
            case KORPORACE:
                parParty = new ParPartyGroup();
                parPartyType = partyTypeRepository.findPartyTypeByCode(PartyType.PARTY_GROUP.getCode());
                break;
            case ROD_RODINA:
                parParty = new ParDynasty();
                parPartyType = partyTypeRepository.findPartyTypeByCode(PartyType.DYNASTY.getCode());
                break;
            case UDÁLOST:
                parParty = new ParEvent();
                parPartyType = partyTypeRepository.findPartyTypeByCode(PartyType.EVENT.getCode());
                break;
            case OSOBA_BYTOST:
                parParty = new ParPerson();
                parPartyType = partyTypeRepository.findPartyTypeByCode(PartyType.PERSON.getCode());
                break;
            default:
                throw new IllegalStateException("Neznámý typ osoby " + trida.value());
        }

        parParty.setPartyType(parPartyType);
        parParty.setRecord(regRecord);
        parParty.setOriginator(isOriginator);

        fillParty(parParty, interpiEntity, regExternalSystem, mappings);

        return parParty;
    }

    /**
     * Načtení vztahů entity.
     *
     * @param regExternalSystem externí systém
     * @param regScope třída
     *
     */
    public List<InterpiRelationMappingVO> getRelations(final InterpiEntity interpiEntity,
            final RegExternalSystem regExternalSystem, final RegScope regScope) {
        List<InterpiRelationMappingVO> mappings = new LinkedList<>();

        List<UdalostTyp> pocatekExistence = interpiEntity.getPocatekExistence();
        List<UdalostTyp> konecExistence = interpiEntity.getKonecExistence();
        List<UdalostTyp> udalostList = interpiEntity.getUdalost();
        List<UdalostTyp> zmenaList = interpiEntity.getZmena();

        addRelationMappings(pocatekExistence, InterpiClass.POCATEK_EXISTENCE, mappings, regExternalSystem, regScope);
        addRelationMappings(konecExistence, InterpiClass.KONEC_EXISTENCE, mappings, regExternalSystem, regScope);
        addRelationMappings(udalostList, InterpiClass.UDALOST, mappings, regExternalSystem, regScope);
        addRelationMappings(zmenaList, InterpiClass.ZMENA, mappings, regExternalSystem, regScope);

        List<SouvisejiciTyp> souvisejiciEntitaList = interpiEntity.getSouvisejiciEntita();
        addEntityMappings(souvisejiciEntitaList, InterpiClass.SOUVISEJICI_ENTITA, mappings, regExternalSystem, regScope);

        return mappings;
    }

    /**
     * Vytvoření mapovacích záznamů pro entity bez vztahu.
     *
     * @param souvisejiciEntitaList seznam entit
     * @param interpiClass třída vztahu
     * @param mappings kolekce do které se přidají vytvořená mapování
     * @param regExternalSystem externí systém
     * @param regScope třída
     */
    private void addEntityMappings(final List<SouvisejiciTyp> souvisejiciEntitaList, final InterpiClass interpiClass,
            final List<InterpiRelationMappingVO> mappings, final RegExternalSystem regExternalSystem, final RegScope regScope) {
        if (CollectionUtils.isEmpty(souvisejiciEntitaList)) {
            return;
        }

        InterpiRelationMappingVO relationMappingVO = createRelationMapping(null, interpiClass); // vztah bez názvu pro navázání entit
        mappings.add(relationMappingVO);

        for (SouvisejiciTyp souvisejiciTyp : souvisejiciEntitaList) {
            InterpiEntityMappingVO entityMappingVO = createEntityMapping(souvisejiciTyp, regExternalSystem, regScope);

            relationMappingVO.addEntityMapping(entityMappingVO);
        }
    }

    /**
     * Vytvoření mapovacích záznamů pro vztahy a jejich entity.
     *
     * @param udalostList seznam vztahů
     * @param interpiClass třída vztahu
     * @param mappings kolekce do které se přidají vytvořená mapování
     * @param regExternalSystem externí systém
     * @param regScope třída
     */
    private void addRelationMappings(final List<UdalostTyp> udalostList, final InterpiClass interpiClass,
            final List<InterpiRelationMappingVO> mappings, final RegExternalSystem regExternalSystem, final RegScope regScope) {
        for (UdalostTyp udalostTyp : udalostList) {
            InterpiRelationMappingVO relationMappingVO = createRelationMapping(udalostTyp, interpiClass);
            mappings.add(relationMappingVO);

            List<SouvisejiciTyp> souvisejiciEntitaList = udalostTyp.getSouvisejiciEntita();
            if (souvisejiciEntitaList != null) {
                for (SouvisejiciTyp souvisejiciTyp : souvisejiciEntitaList) {
                    InterpiEntityMappingVO entityMappingVO = createEntityMapping(souvisejiciTyp, regExternalSystem, regScope);

                    relationMappingVO.addEntityMapping(entityMappingVO);
                }
            }
        }
    }

    /**
     * Vytvoří mapovací záznam pro vztah.
     *
     * @param udalostTyp vztah
     * @param interpiClass třída vztahu
     *
     * @return mapovací záznam
     */
    private InterpiRelationMappingVO createRelationMapping(final UdalostTyp udalostTyp, final InterpiClass interpiClass) {
        InterpiRelationMappingVO mappingVO = new InterpiRelationMappingVO();

        mappingVO.setInterpiClass(interpiClass);
        mappingVO.setImportRelation(true);

        if (udalostTyp != null) {
            mappingVO.setInterpiRelationType(udalostTyp.getTyp().value());
        }

        return mappingVO;
    }


    /**
     * Vytvoří mapovací záznam pro entitu.
     *
     * @param souvisejiciTyp entita
     * @param regExternalSystem externí systém
     * @param regScope třída
     *
     * @return mapovací záznam
     */
    private InterpiEntityMappingVO createEntityMapping(final SouvisejiciTyp souvisejiciTyp, final RegExternalSystem regExternalSystem, final RegScope regScope) {
        InterpiEntityMappingVO entityMappingVO = new InterpiEntityMappingVO();

        String interpiRole = getInterpiRoleType(souvisejiciTyp);
        entityMappingVO.setInterpiRoleType(interpiRole);

        OznaceniTyp preferovaneOznaceni = souvisejiciTyp.getPreferovaneOznaceni();
        String entityName = preferovaneOznaceni.getHlavniCast().getValue();
        VedlejsiCastTyp vedlejsiCast = preferovaneOznaceni.getVedlejsiCast();
        if (vedlejsiCast != null && StringUtils.isNotBlank(vedlejsiCast.getValue())) {
            entityName += " " + vedlejsiCast.getValue();
        }
        entityMappingVO.setInterpiEntityName(entityName);

        String interpiIdentifier = getInterpiSouvIdentifier(souvisejiciTyp.getIdentifikator());
        entityMappingVO.setInterpiId(interpiIdentifier);

        String interpiEntityType = getInterpiEntityType(souvisejiciTyp, interpiIdentifier, regExternalSystem, regScope);
        entityMappingVO.setInterpiEntityType(interpiEntityType);

        if (interpiEntityType != null) {
            String registryTypeCode = getRegistryTypeCode(interpiEntityType);
            RegRegisterType regRegisterType = registerTypeRepository.findRegisterTypeByCode(registryTypeCode);
            if (regRegisterType == null) {
                entityMappingVO.setNotExistingType(true);
            }
        }

        entityMappingVO.setImportEntity(true);

        return entityMappingVO;
    }

    private String getInterpiEntityType(final SouvisejiciTyp souvisejiciTyp, final String interpiIdentifier,
            final RegExternalSystem regExternalSystem, final RegScope regScope) {
        String interpiEntityType = null;
        if (StringUtils.isNotBlank(interpiIdentifier)) {
            RegRecord regRecord = recordRepository.findRegRecordByExternalIdAndExternalSystemCodeAndScope(interpiIdentifier,
                    regExternalSystem.getCode(), regScope);
            if (regRecord == null) {
                // najít v interpi
                EntitaTyp entitaTyp = client.findOneRecord(interpiIdentifier, regExternalSystem);
                interpiSessionHolder.getInterpiEntitySession().addRelatedEntity(interpiIdentifier, entitaTyp);
                InterpiEntity interpiEntity = new InterpiEntity(entitaTyp);
                PodtridaTyp podTrida = interpiEntity.getPodTrida();
                if (podTrida == null) {
                    TridaTyp trida = interpiEntity.getTrida();
                    if (trida != null) {
                        interpiEntityType = trida.value();
                    }
                } else {
                    interpiEntityType = podTrida.value();
                }
            } else {
                interpiEntityType = regRecord.getRegisterType().getName();
            }
        } else {
            TridaTyp trida = souvisejiciTyp.getTrida();
            if (trida != null) {
                interpiEntityType = trida.value();
            }
        }

        return interpiEntityType;
    }

    private String getInterpiRoleType(final SouvisejiciTyp souvisejiciTyp) {
        String interpiRole = null;
        RoleTypA role = souvisejiciTyp.getRole();
        if (role == null) {
            interpiRole = "související entita";
        } else {
            interpiRole = role.value();
        }
        return interpiRole;
    }
}
