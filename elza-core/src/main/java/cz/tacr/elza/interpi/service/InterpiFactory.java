package cz.tacr.elza.interpi.service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.InterpiClass;
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
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.service.vo.MappingVO;
import cz.tacr.elza.interpi.ws.wo.DoplnekTyp;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikaceTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorSouvTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorSouvTypA;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorTypA;
import cz.tacr.elza.interpi.ws.wo.KodovaneTyp;
import cz.tacr.elza.interpi.ws.wo.KodovaneTypA;
import cz.tacr.elza.interpi.ws.wo.KomplexniDataceTyp;
import cz.tacr.elza.interpi.ws.wo.KomplexniDataceTypA;
import cz.tacr.elza.interpi.ws.wo.OznaceniTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTypTypA;
import cz.tacr.elza.interpi.ws.wo.PodtridaTyp;
import cz.tacr.elza.interpi.ws.wo.PopisTyp;
import cz.tacr.elza.interpi.ws.wo.PravidlaTyp;
import cz.tacr.elza.interpi.ws.wo.RoleTypA;
import cz.tacr.elza.interpi.ws.wo.SouradniceTyp;
import cz.tacr.elza.interpi.ws.wo.SouvisejiciTyp;
import cz.tacr.elza.interpi.ws.wo.StrukturaTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTypA;
import cz.tacr.elza.interpi.ws.wo.TridaTyp;
import cz.tacr.elza.interpi.ws.wo.UdalostTyp;
import cz.tacr.elza.interpi.ws.wo.UdalostTypA;
import cz.tacr.elza.interpi.ws.wo.VedlejsiCastTyp;
import cz.tacr.elza.interpi.ws.wo.VyobrazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZarazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZaznamTyp;
import cz.tacr.elza.interpi.ws.wo.ZdrojTyp;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.PartyType;
import liquibase.util.file.FilenameUtils;

/**
 * Třída pro konverzi objektů z a do INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 6. 12. 2016
 */
@Service
public class InterpiFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Výchozí skript mapování rejstříkového hesla.
     */
    @Value("classpath:script/groovy/interpiRecord.groovy")
    private Resource createDetailDefaultResource;

    /**
     * Načtená transformace pro vytvoření detailu rejstříkového hesla.
     */
    private Resource createDetailResource;

    private static final String RECORD_DETAIL_FILE = "interpiRecord.groovy";

    @Value("${elza.groovy.groovyDir}")
    private String groovyScriptDir;

    @Autowired
    private InterpiSessionHolder interpiSessionHolder;

    @Autowired
    private PartyService partyService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private ScriptEvaluator groovyScriptEvaluator;

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
    private VariantRecordRepository variantRecordRepository;

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
     * @param valueMap INTERPI objekt
     * @param originalRecord původní rejstřík, může být null
     * @param interpiRecordId id INTERPI
     * @param isOriginator příznak zda je osoba původce
     * @param regScope třída rejstříku
     * @param regExternalSystem externí systém
     *
     * @return uložené rejstříkové heslo osoby
     */
    public RegRecord importParty(final Map<EntityValueType, List<Object>> valueMap, final RegRecord originalRecord,
            final String interpiRecordId, final boolean isOriginator, final RegScope regScope,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        RegRecord regRecord = createPartyRecord(valueMap, interpiRecordId, regExternalSystem, regScope);

        Integer partyId = null;
        Integer partyVersion = null;
        if (originalRecord != null) {
            regRecord.setRecordId(originalRecord.getRecordId());
            regRecord.setVersion(originalRecord.getVersion());

            ParParty originalParty = partyService.findParPartyByRecord(originalRecord);

            List<ParRelation> relations = new ArrayList<>(originalParty.getRelations());
            if (CollectionUtils.isNotEmpty(relations)) {
                for (ParRelation relation : relations) {
                    partyService.deleteRelation(relation);
                }
            }
            originalParty.setRelations(null);
            partyService.saveParty(originalParty);

            partyId = originalParty.getPartyId();
            partyVersion = originalParty.getVersion();
        }

        ParParty newParty = createParty(regRecord, valueMap, isOriginator, regExternalSystem, mappings);
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

    public List<ExternalRecordVO> convertToExternalRecordVO(final List<EntitaTyp> searchResults,
            final boolean generateVariantNames) {
        Map<String, Object> input = new HashMap<>();
        input.put("ENTITIES", searchResults);
        input.put("FACTORY", this);
        input.put("GENERATE_VARIANT_NAMES", generateVariantNames);

        ScriptSource source = new ResourceScriptSource(createDetailResource);
        return (List<ExternalRecordVO>) groovyScriptEvaluator.evaluate(source, input);
    }

    public String getInterpiRecordId(final Map<EntityValueType, List<Object>> valueMap) {
        List<IdentifikaceTyp> identifikace = getIdentifikace(valueMap);
        String interpiRecordId = getInterpiIdentifier(identifikace);

        if (interpiRecordId == null) {
            throw new SystemException(BaseCode.ID_NOT_EXIST);
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
            throw new SystemException(BaseCode.ID_NOT_EXIST);
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
        Map<EntityValueType, List<Object>> valueMap = convertToMap(entitaTyp);
        RegRecord regRecord = createPartyRecord(valueMap, interpiPartyId, regExternalSystem, regScope);

        ExternalRecordVO recordVO = convertToExternalRecordVO(Collections.singletonList(entitaTyp), generateVariantNames).
                iterator().next();

        regRecord.setCharacteristics(recordVO.getDetail());
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
     * @param valueMap mapa hodnot z interpi
     * @param interpiPartyId extrní id osoby
     * @param regExternalSystem systém ze kterého je osoba
     * @param regScope třída rejstříků do které se importuje
     *
     * @return rejstříkové heslo
     */
    public RegRecord createPartyRecord(final Map<EntityValueType, List<Object>> valueMap,
            final String interpiPartyId, final RegExternalSystem regExternalSystem, final RegScope regScope) {
        RegRecord regRecord = new RegRecord();

        regRecord.setExternalId(interpiPartyId);
        regRecord.setExternalSystem(regExternalSystem);

        String note = getNote(valueMap);
        regRecord.setNote(note);

        RegRegisterType regRegisterType = getRegisterType(valueMap);
        regRecord.setRegisterType(regRegisterType);

        regRecord.setScope(regScope);

        return regRecord;
    }

    /**
     * Vytvoří poznámku.
     *
     * @param valueMap mapa hodnot z interpi
     *
     * @return poznámka, může být null
     */
    private String getNote(final Map<EntityValueType, List<Object>> valueMap) {
        List<String> notes = new LinkedList<>();
        List<PopisTyp> popisTypList = getPopisTyp(valueMap);
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

    private String getSourceInformation(final Map<EntityValueType, List<Object>> valueMap) {
        List<ZdrojTyp> zdrojTypList = getZdrojTyp(valueMap);
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
     * @param valueMap hodnoty entity
     *
     * @return typ rejstříku
     */
    public RegRegisterType getRegisterType(final Map<EntityValueType, List<Object>> valueMap) {
        String registryTypeName;
        PodtridaTyp podTrida = getPodTrida(valueMap);
        if (podTrida == null) {
            TridaTyp trida = getTrida(valueMap);
            registryTypeName = trida.value();
        } else {
            registryTypeName = podTrida.value();
        }

        RegRegisterType regRegisterType = registerTypeRepository.findRegisterTypeByName(registryTypeName);
        if (regRegisterType == null) {
            throw new ObjectNotFoundException(RegistryCode.REGISTRY_TYPE_NOT_FOUND).set("name", registryTypeName);
        }

        return regRegisterType;
    }

    /**
     * Naplní předanou osobu.
     *
     * @param parParty osoba
     * @param valueMap informace z INTERPI
     * @param regExternalSystem externí systém
     * @param mappings mapování vztahů
     */
    private void fillParty(final ParParty parParty, final Map<EntityValueType, List<Object>> valueMap,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
//        parParty.setPartyCreators(null); // po dohodě s Honzou Vejskalem neimportovat, není jak

        List<ParPartyName> partyNames = new LinkedList<>();
        List<OznaceniTyp> variantniOznaceniList = getVariantniOznaceni(valueMap);
        if (CollectionUtils.isNotEmpty(variantniOznaceniList)) {
            for (OznaceniTyp variantniOznaceni : variantniOznaceniList) {
                ParPartyName parPartyName = createPartyName(valueMap, variantniOznaceni, parParty, false);
                if (parPartyName != null) {
                    partyNames.add(parPartyName);
                }
            }
        }
        parParty.setPartyNames(partyNames);

        OznaceniTyp preferovaneOznaceni = getPreferovaneOznaceni(valueMap);
        ParPartyName preferredName = createPartyName(valueMap, preferovaneOznaceni, parParty, true);
        parParty.setPreferredName(preferredName);
        partyNames.add(preferredName);

        String sourceInformation = getSourceInformation(valueMap);
        parParty.setSourceInformation(sourceInformation);

        fillAdditionalInfo(parParty, valueMap);

        if (parParty.isOriginator() && CollectionUtils.isNotEmpty(mappings)) {
            fillRelations(parParty, valueMap, regExternalSystem, mappings);
        }
    }

    private void fillRelations(final ParParty parParty, final Map<EntityValueType, List<Object>> valueMap,
            final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        List<UdalostTyp> pocatekExistence = getPocatekExistence(valueMap);
        List<UdalostTyp> konecExistence = getKonecExistence(valueMap);
        List<UdalostTyp> udalostList = getUdalost(valueMap);
        List<UdalostTyp> zmenaList = getZmena(valueMap);
        List<SouvisejiciTyp> souvisejiciEntitaList = getSouvisejiciEntita(valueMap);

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
            if (mappingVO == null) {
                throw new IllegalStateException("Pro entitu " + interpiRoleType + " nebylo nalezeno mapování.");
            }

            if (!mappingVO.isImportRelation()) { // přeskočení
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

            if (CollectionUtils.isEmpty(souvisejiciEntitaList)) {
                MappingVO mappingVO = findRelationMapping(mappings, interpiClass, interpiRelationType, null, null);
                if (mappingVO == null) {
                    throw new IllegalStateException("Pro vztah " + interpiRelationType + " nebylo nalezeno mapování.");
                }

                if (!mappingVO.isImportRelation()) { // přeskočení
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
                    if (mappingVO == null) {
                        throw new IllegalStateException("Pro vztah " + interpiRoleType + " a roli " + interpiRoleType + " nebylo nalezeno mapování.");
                    }

                    if (!mappingVO.isImportRelation()) { // přeskočení
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
        Assert.isTrue(interpiRelationType != null || interpiRoleType != null);

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
                Map<EntityValueType, List<Object>> entityValueMap = convertToMap(entitaTyp);
                if (isParty(entityValueMap)) {
                    entityRecord = importParty(entityValueMap, null, interpiId, false, parParty.getRecord().getScope(), regExternalSystem, null);
                } else {
                    entityRecord = importRecord(entitaTyp, null, interpiId, parParty.getRecord().getScope(), regExternalSystem);
                }
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
        } else if (dataceTo != null) {
            unitdateTo = convertDataceToParUnitdate(dataceTo);
        } else if (datace1 != null && unitdateFrom == null) {
            unitdateFrom = convertDataceToParUnitdate(datace1);
        } else if (datace1 != null && unitdateTo == null) {
            unitdateTo = convertDataceToParUnitdate(datace1);
        } else if (datace2 != null && unitdateTo == null) {
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

        UnitDateConvertor.convertToUnitDate(dataceTyp.getTextDatace(), parUnitdate);
        parUnitdate.setNote(dataceTyp.getPoznamka());

        return parUnitdate;
    }

    /**
     * Nastaví různé informace které jsou v INTERPI uloženy jako popis.
     */
    private void fillAdditionalInfo(final ParParty newParty, final Map<EntityValueType, List<Object>> valueMap) {
        List<PopisTyp> popisTypList = getPopisTyp(valueMap);

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

            List<KodovaneTyp> kodovaneUdajeList = getKodovaneUdaje(valueMap);
            List<ParPartyGroupIdentifier> partyGroupIdentifiers = new ArrayList<>(kodovaneUdajeList.size());
            parPartyGroup.setPartyGroupIdentifiers(partyGroupIdentifiers);
            for (KodovaneTyp kodovaneTyp : kodovaneUdajeList) {
                ParPartyGroupIdentifier partyGroupIdentifier = new ParPartyGroupIdentifier();
                partyGroupIdentifiers.add(partyGroupIdentifier);

                String datace = kodovaneTyp.getDatace();
                if (StringUtils.isNotBlank(datace)) {
                    ParUnitdate parUnitdate = new ParUnitdate();
                    UnitDateConvertor.convertToUnitDate(datace, parUnitdate);
                    partyGroupIdentifier.setFrom(parUnitdate);
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

    private ParPartyName createPartyName(final Map<EntityValueType, List<Object>> valueMap, final OznaceniTyp oznaceniTyp, final ParParty parParty, final boolean isPreferred) {
        ParPartyName partyName = new ParPartyName();

        ParPartyNameFormType parPartyNameFormType = null;
        OznaceniTypTypA typ = oznaceniTyp.getTyp();
        if (typ != null) {
            String partyNameFormTypeName = typ.value();
            parPartyNameFormType = partyNameFormTypeRepository.findByName(partyNameFormTypeName);
            if (parPartyNameFormType == null) {
                throw new ObjectNotFoundException(ArrangementCode.PARTY_NAME_FORM_TYPE_NOT_FOUND).set("name", partyNameFormTypeName);
            }
        }
        partyName.setNameFormType(parPartyNameFormType);

        if (isPreferred) {
            Set<String> degreesBefore = new HashSet<>();
            Set<String> degreesAfter = new HashSet<>();
            List<TitulTyp> titulTypList = getTitul(valueMap);
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
    public boolean isParty(final Map<EntityValueType, List<Object>> valueMap) {
        TridaTyp trida = getTrida(valueMap);

        List<TridaTyp> partyTypes = Arrays.asList(TridaTyp.KORPORACE, TridaTyp.OSOBA_BYTOST, TridaTyp.ROD_RODINA, TridaTyp.UDÁLOST);

        return partyTypes.contains(trida);
    }

    /**
     * Vytvoří osobu.
     *
     * @param regRecord rejstříkové heslo osoby
     * @param valueMap data osoby
     * @param isOriginator příznak původce
     * @param regExternalSystem systém ze kterého je osoba
     * @param mappings mapování vztahů
     *
     * @return osoba
     */
    private ParParty createParty(final RegRecord regRecord, final Map<EntityValueType, List<Object>> valueMap,
            final boolean isOriginator, final RegExternalSystem regExternalSystem, final List<MappingVO> mappings) {
        TridaTyp trida = getTrida(valueMap);

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

        fillParty(parParty, valueMap, regExternalSystem, mappings);

        return parParty;
    }

    /**
     * Převod entity do mapy, kde klíče jsou typy hodnot.
     *
     * @param entitaTyp INTERPI entita
     *
     * @return mapa hodnot, typ hodnoty -> seznam hodnot
     */
    public Map<EntityValueType, List<Object>> convertToMap(final EntitaTyp entitaTyp) {
        Assert.notNull(entitaTyp);
        Assert.notNull(entitaTyp.getContent());

        Map<EntityValueType, List<Object>> result = new HashMap<>();
        for (Serializable ser : entitaTyp.getContent()) {
            JAXBElement<?> element = (JAXBElement<?>) ser;
            addToMap(element, result);
        }

        return result;
    }

    private void addToMap(final JAXBElement<?> element, final Map<EntityValueType, List<Object>> result) {
        Object value = element.getValue();
        String localPart = element.getName().getLocalPart();
        switch (localPart) {
            case "trida":
                TridaTyp tridaTyp = getEntity(value, TridaTyp.class);
                putToMap(result, EntityValueType.TRIDA, tridaTyp);
                break;
            case "podtrida":
                PodtridaTyp podtridaTyp = getEntity(value, PodtridaTyp.class);
                putToMap(result, EntityValueType.PODTRIDA, podtridaTyp);
                break;
            case "identifikace":
                IdentifikaceTyp identifikaceTyp = getEntity(value, IdentifikaceTyp.class);
                putToMap(result, EntityValueType.IDENTIFIKACE, identifikaceTyp);
                break;
            case "zaznam":
                ZaznamTyp zaznamTyp = getEntity(value, ZaznamTyp.class);
                putToMap(result, EntityValueType.ZAZNAM, zaznamTyp);
                break;
            case "preferovane_oznaceni":
                OznaceniTyp prefereovane = getEntity(value, OznaceniTyp.class);
                putToMap(result, EntityValueType.PREFEROVANE_OZNACENI, prefereovane);
                break;
            case "variantni_oznaceni":
                OznaceniTyp variantni = getEntity(value, OznaceniTyp.class);
                putToMap(result, EntityValueType.VARIANTNI_OZNACENI, variantni);
                break;
            case "udalost":
                UdalostTyp udalostTyp = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.UDALOST, udalostTyp);
                break;
            case "pocatek_existence":
                UdalostTyp pocatek = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.POCATEK_EXISTENCE, pocatek);
                break;
            case "konec_existence":
                UdalostTyp konec = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.KONEC_EXISTENCE, konec);
                break;
            case "zmena":
                UdalostTyp zmena = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.ZMENA, zmena);
                break;
            case "popis":
                PopisTyp popisTyp = getEntity(value, PopisTyp.class);
                putToMap(result, EntityValueType.POPIS, popisTyp);
                break;
            case "souradnice":
                SouradniceTyp souradniceTyp = getEntity(value, SouradniceTyp.class);
                putToMap(result, EntityValueType.SOURADNICE, souradniceTyp);
                break;
            case "titul":
                TitulTyp titulTyp = getEntity(value, TitulTyp.class);
                putToMap(result, EntityValueType.TITUL, titulTyp);
                break;
            case "kodovane_udaje":
                KodovaneTyp kodovaneTyp = getEntity(value, KodovaneTyp.class);
                putToMap(result, EntityValueType.KODOVANE_UDAJE, kodovaneTyp);
                break;
            case "souvisejici_entita":
                SouvisejiciTyp souvisejiciTyp = getEntity(value, SouvisejiciTyp.class);
                putToMap(result, EntityValueType.SOUVISEJICI_ENTITA, souvisejiciTyp);
                break;
            case "hierarchicka_struktura":
                StrukturaTyp strukturaTyp = getEntity(value, StrukturaTyp.class);
                putToMap(result, EntityValueType.HIERARCHICKA_STRUKTURA, strukturaTyp);
                break;
            case "zarazeni":
                ZarazeniTyp zarazeniTyp = getEntity(value, ZarazeniTyp.class);
                putToMap(result, EntityValueType.ZARAZENI, zarazeniTyp);
                break;
            case "vyobrazeni":
                VyobrazeniTyp vyobrazeniTyp = getEntity(value, VyobrazeniTyp.class);
                putToMap(result, EntityValueType.VYOBRAZENI, vyobrazeniTyp);
                break;
            case "zdroj_informaci":
                ZdrojTyp zdrojTyp = getEntity(value, ZdrojTyp.class);
                putToMap(result, EntityValueType.ZDROJ_INFORMACI, zdrojTyp);
                break;
        }
    }

    private void putToMap(final Map<EntityValueType, List<Object>> result, final EntityValueType type, final Object value) {
        List<Object> values = result.get(type);
        if (values == null) {
            values = new LinkedList<>();
            result.put(type, values);
        }
        values.add(value);
    }

    private <T> T getEntity(final Object value, final Class<T> cls) {
        return cls.cast(value);
    }

    public List<PopisTyp> getPopisTyp(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.POPIS);
    }

    public List<ZdrojTyp> getZdrojTyp(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.ZDROJ_INFORMACI);
    }

    public List<TitulTyp> getTitul(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.TITUL);
    }

    public List<IdentifikaceTyp> getIdentifikace(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.IDENTIFIKACE);
    }

    public List<OznaceniTyp> getVariantniOznaceni(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.VARIANTNI_OZNACENI);
    }

    public List<UdalostTyp> getPocatekExistence(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.POCATEK_EXISTENCE);
    }

    public List<UdalostTyp> getKonecExistence(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.KONEC_EXISTENCE);
    }

    public List<UdalostTyp> getUdalost(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.UDALOST);
    }

    public List<UdalostTyp> getZmena(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.ZMENA);
    }

    public List<SouvisejiciTyp> getSouvisejiciEntita(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.SOUVISEJICI_ENTITA);
    }

    public List<KodovaneTyp> getKodovaneUdaje(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.KODOVANE_UDAJE);
    }

    public OznaceniTyp getPreferovaneOznaceni(final Map<EntityValueType, List<Object>> valueMap) {
        List<OznaceniTyp> preferovaneOznaceniList = getValueList(valueMap, EntityValueType.PREFEROVANE_OZNACENI);

        // chceme typ ZP, pak INTERPI a pak je to jedno
        OznaceniTyp zp = null;
        OznaceniTyp interpi = null;
        OznaceniTyp other = null;
        for (OznaceniTyp oznaceniTyp : preferovaneOznaceniList) {
            for (PravidlaTyp pravidlaTyp : oznaceniTyp.getPravidla()) {
                switch (pravidlaTyp) {
                    case INTERPI:
                        if (interpi == null) {
                            interpi = oznaceniTyp;
                        }
                        break;
                    case ZP:
                        if (zp == null) {
                            zp = oznaceniTyp;
                        }
                        break;
                    default:
                        if (other == null) {
                            other = oznaceniTyp;
                        }
                }
            }
        }

        // alespoň jedno bude podle xsd vyplněné
        OznaceniTyp preferovaneOznaceni;
        if (zp != null) {
            preferovaneOznaceni = zp;
        } else if (interpi != null) {
            preferovaneOznaceni = interpi;
        } else {
            preferovaneOznaceni = other;
        }

        return preferovaneOznaceni;
    }

    public PodtridaTyp getPodTrida(final Map<EntityValueType, List<Object>> valueMap) {
        return getValue(valueMap, EntityValueType.PODTRIDA);
    }

    public TridaTyp getTrida(final Map<EntityValueType, List<Object>> valueMap) {
        return getValue(valueMap, EntityValueType.TRIDA);
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(final Map<EntityValueType, List<Object>> valueMap, final EntityValueType entityValueType) {
        List<Object> valuesList = valueMap.get(entityValueType);

        if (CollectionUtils.isEmpty(valuesList)) {
            return null;
        }
        return (T) valuesList.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getValueList(final Map<EntityValueType, List<Object>> valueMap, final EntityValueType entityValueType) {
        List<Object> valuesList = valueMap.get(entityValueType);

        if (CollectionUtils.isEmpty(valuesList)) {
            return Collections.emptyList();
        }

        return valuesList.stream().
                map(v -> (T) v).
                collect(Collectors.toList());
    }

    @PostConstruct
    private void initScripts() {
        File dirRules = new File(groovyScriptDir);
        if (!dirRules.exists()) {
            dirRules.mkdir();
        }

        File createTransformationFile = new File(FilenameUtils.concat(groovyScriptDir, RECORD_DETAIL_FILE));

        try {
            if (!createTransformationFile.exists() || createTransformationFile.lastModified() < createDetailDefaultResource
                    .lastModified()) {
                Files.copy(createDetailDefaultResource.getInputStream(), createTransformationFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                File copiedFile = new File(createTransformationFile.getAbsolutePath());
                copiedFile.setLastModified(createDetailDefaultResource.lastModified());


                logger.info("Vytvoření souboru " + createTransformationFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit soubor " + createTransformationFile.getAbsolutePath(), e);
        }

        createDetailResource = new PathResource(createTransformationFile.toPath());
    }

    /**
     * Načtení vztahů entity.
     *
     * @param valueMap data entity
     * @param regExternalSystem externí systém
     * @param regScope třída
     *
     * @param seznam mapování
     */
    public List<InterpiRelationMappingVO> getRelations(final Map<EntityValueType, List<Object>> valueMap,
            final RegExternalSystem regExternalSystem, final RegScope regScope) {
        List<InterpiRelationMappingVO> mappings = new LinkedList<>();

        List<UdalostTyp> pocatekExistence = getPocatekExistence(valueMap);
        List<UdalostTyp> konecExistence = getKonecExistence(valueMap);
        List<UdalostTyp> udalostList = getUdalost(valueMap);
        List<UdalostTyp> zmenaList = getZmena(valueMap);

        addRelationMappings(pocatekExistence, InterpiClass.POCATEK_EXISTENCE, mappings, regExternalSystem, regScope);
        addRelationMappings(konecExistence, InterpiClass.KONEC_EXISTENCE, mappings, regExternalSystem, regScope);
        addRelationMappings(udalostList, InterpiClass.UDALOST, mappings, regExternalSystem, regScope);
        addRelationMappings(zmenaList, InterpiClass.ZMENA, mappings, regExternalSystem, regScope);

        List<SouvisejiciTyp> souvisejiciEntitaList = getSouvisejiciEntita(valueMap);
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
        entityMappingVO.setInterpiEntityName(preferovaneOznaceni.getHlavniCast().getValue() + " " + preferovaneOznaceni.getVedlejsiCast().getValue());

        String interpiIdentifier = getInterpiSouvIdentifier(souvisejiciTyp.getIdentifikator());
        entityMappingVO.setInterpiId(interpiIdentifier);

        String interpiEntityType = getInterpiEntityType(souvisejiciTyp, interpiIdentifier, regExternalSystem, regScope);
        entityMappingVO.setInterpiEntityType(interpiEntityType);

        if (interpiEntityType != null) {
            RegRegisterType regRegisterType = registerTypeRepository.findRegisterTypeByName(interpiEntityType);
            if (regRegisterType == null) {
                entityMappingVO.setNotExistingType(true);
            }
        }

        entityMappingVO.setImportEntity(true);

        entityMappingVO.setInterpiEntityName(entityMappingVO.getInterpiEntityName() + interpiEntityType);


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
                Map<EntityValueType, List<Object>> valueMap = convertToMap(entitaTyp);
                PodtridaTyp podTrida = getPodTrida(valueMap);
                if (podTrida == null) {
                    TridaTyp trida = getTrida(valueMap);
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
//            interpiRole = "test"; // TODO nekomitovat
            throw new IllegalStateException("Související entita nemá vyplněn typ role.");
        } else {
            interpiRole = role.value();
        }
        return interpiRole;
    }
}
