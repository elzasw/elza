package cz.tacr.elza.interpi.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.interpi.service.pqf.PQFQueryBuilder;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.ws.wo.DoplnekTyp;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikaceTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorTypA;
import cz.tacr.elza.interpi.ws.wo.KodovaneTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTypTypA;
import cz.tacr.elza.interpi.ws.wo.PodtridaTyp;
import cz.tacr.elza.interpi.ws.wo.PopisTyp;
import cz.tacr.elza.interpi.ws.wo.PravidlaTyp;
import cz.tacr.elza.interpi.ws.wo.SouradniceTyp;
import cz.tacr.elza.interpi.ws.wo.SouvisejiciTyp;
import cz.tacr.elza.interpi.ws.wo.StrukturaTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTypA;
import cz.tacr.elza.interpi.ws.wo.TridaTyp;
import cz.tacr.elza.interpi.ws.wo.UdalostTyp;
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
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.utils.PartyType;
import cz.tacr.elza.utils.XmlUtils;
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
     * Výchozí transformace pro vytvoření detailu rejstříkového hesla.
     */
    @Value("classpath:/interpi/transformation/detail.xslt")
    private Resource createDetailDefaultResource;

    /**
     * Načtená transformace pro vytvoření detailu rejstříkového hesla.
     */
    private Resource createDetailResource;

    private static final String CREATE_TRANSFORMATION_FILE = "detail.xslt";

    @Value("${elza.interpi.interpiDir}")
    private String interpiDir;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    public String createSearchQuery(final List<ConditionVO> conditions, final boolean isParty) {
        return new PQFQueryBuilder(conditions).
                extend().
                party(isParty).
                createQuery();
    }

    public ExternalRecordVO convertToExternalRecordVO(final EntitaTyp entitaTyp, final RegExternalSystem regExternalSystem) {
        Map<EntityValueType, List<Object>> valueMap = convertToMap(entitaTyp);

        String interpiRecordId = getInterpiRecordId(valueMap);
        RegRecord recordFromGroovy = createGroovyRecord(regExternalSystem, valueMap, interpiRecordId);
        String detail = createDetail(entitaTyp);

        ExternalRecordVO recordVO = new ExternalRecordVO();
        recordVO.setDetail(detail);
        recordVO.setName(recordFromGroovy.getRecord());
        recordVO.setRecordId(interpiRecordId);

        List<RegRecord> regRecords = recordRepository.findRegRecordByExternalIdAndExternalSystemCode(interpiRecordId, regExternalSystem.getCode());
        for (RegRecord existingRecord : regRecords) {
            recordVO.addPairedRecord(existingRecord);
        }

        return recordVO;
    }

    private RegRecord createGroovyRecord(final RegExternalSystem regExternalSystem,
            final Map<EntityValueType, List<Object>> valueMap, final String interpiRecordId) {
        RegScope scope = scopeRepository.findByCode("GLOBAL");
        RegRecord regRecord = createRecord(valueMap, interpiRecordId, regExternalSystem, scope);

        ParParty newParty = createParty(regRecord, valueMap, true);

        return groovyScriptService.getRecordFromGroovy(newParty);
    }

    private String createDetail(final EntitaTyp entitaTyp) {
        byte[] marshallData = XmlUtils.marshallData(entitaTyp, EntitaTyp.class);

        if (createDetailResource != null) {
            byte[] data = XmlUtils.transformData(marshallData, createDetailResource);
            return new String(data, Charset.forName("UTF-8"));
        } else {
            return XmlUtils.formatXml(new ByteArrayInputStream(marshallData));
        }
    }

    private String getInterpiRecordId(final Map<EntityValueType, List<Object>> valueMap) {
        String interpiRecordId = null;
        List<IdentifikaceTyp> identifikace = getIdentifikace(valueMap);
        for (IdentifikaceTyp identifikaceTyp : identifikace) {
            for (IdentifikatorTyp identifikator : identifikaceTyp.getIdentifikator()) {
                if (identifikator.getTyp().equals(IdentifikatorTypA.INTERPI)) {
                    interpiRecordId = identifikator.getValue();
                    break;
                }
            }

            if (interpiRecordId != null) {
                break;
            }
        }

        if (interpiRecordId == null) {
            throw new IllegalStateException("Nebyl nalezen identifikátor INTERPI.");
        }
        return interpiRecordId;
    }

    public RegRecord createRecord(final Map<EntityValueType, List<Object>> valueMap,
            final String interpiPartyId, final RegExternalSystem regExternalSystem, final RegScope regScope) {
        RegRecord regRecord = new RegRecord();

//        regRecord.setCharacteristics(characteristics);
        regRecord.setExternalId(interpiPartyId);
        regRecord.setExternalSystem(regExternalSystem);

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
        regRecord.setNote(note);
//        regRecord.setParentRecord(parentRecord);
//        regRecord.setRecord(record);

        TridaTyp trida = getTrida(valueMap);
        PodtridaTyp podTrida = getPodTrida(valueMap);
        RegRegisterType regRegisterType;
        if (podTrida == null) {
            regRegisterType = registerTypeRepository.findRegisterTypeByname(trida.value());
        } else {
            regRegisterType = registerTypeRepository.findRegisterTypeByname(podTrida.value());
        }

        regRecord.setRegisterType(regRegisterType);
        regRecord.setScope(regScope);
//        regRecord.setVariantRecordList(variantRecordList);

        return regRecord;
    }

    private void fillParty(final ParParty newParty, final Map<EntityValueType, List<Object>> valueMap) {
//        newParty.setPartyCreators(partyCreators);

        List<ParPartyName> partyNames = new LinkedList<>();
        List<OznaceniTyp> variantniOznaceniList = getVariantniOznaceni(valueMap);
        if (CollectionUtils.isNotEmpty(variantniOznaceniList)) {
            for (OznaceniTyp variantniOznaceni : variantniOznaceniList) {
                ParPartyName parPartyName = createPartyName(valueMap, variantniOznaceni, newParty, false);
                if (parPartyName != null) {
                    partyNames.add(parPartyName);
                }
            }
        }
        newParty.setPartyNames(partyNames);

        OznaceniTyp preferovaneOznaceni = getPreferovaneOznaceni(valueMap);
        ParPartyName preferredName = createPartyName(valueMap, preferovaneOznaceni, newParty, true);
        newParty.setPreferredName(preferredName);
        partyNames.add(preferredName);

//        newParty.setRecord(record);
//        newParty.setRelations(relations);

        String sourceInformation;
        List<ZdrojTyp> zdrojTypList = getZdrojTyp(valueMap);
        if (CollectionUtils.isEmpty(zdrojTypList)) {
            sourceInformation = null;
        } else {
            List<String> sourceInformations = new ArrayList<>(zdrojTypList.size());
            for (ZdrojTyp zdrojTyp : zdrojTypList) {
                if (StringUtils.isNotBlank(zdrojTyp.getStandardizovanyPopis())) {
                    sourceInformations.add(StringUtils.trim(zdrojTyp.getStandardizovanyPopis()));
                }
            }

            sourceInformation = StringUtils.join(sourceInformations, ", ");
        }
        newParty.setSourceInformation(sourceInformation);

        fillPopis(newParty, valueMap);

    }

    private void fillPopis(final ParParty newParty, final Map<EntityValueType, List<Object>> valueMap) {
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

//            String foundingNorm = null;
            String foundingNorm = "d"; // TODO jen pro předváděčku
            if (!foundingNormList.isEmpty()) {
                foundingNorm = StringUtils.join(foundingNormList, ", ");
            }
            parPartyGroup.setFoundingNorm(foundingNorm);

//            String organization = null;
            String organization = "d"; // TODO jen pro předváděčku
            if (!organizationList.isEmpty()) {
                organization = StringUtils.join(organizationList, ", ");
            }
            parPartyGroup.setOrganization(organization);

//            String scope = null;
            String scope = "d"; // TODO jen pro předváděčku
            if (!scopeList.isEmpty()) {
                scope = StringUtils.join(scopeList, ", ");
            }
            parPartyGroup.setScope(scope);

//            String scopeNorm = null;
            String scopeNorm = "d"; // TODO jen pro předváděčku
            if (!scopeNormList.isEmpty()) {
                scopeNorm = StringUtils.join(scopeNormList, ", ");
            }
            parPartyGroup.setScopeNorm(scopeNorm);
        }
    }

    private ParPartyName createPartyName(final Map<EntityValueType, List<Object>> valueMap, final OznaceniTyp oznaceniTyp, final ParParty parParty, final boolean isPreferred) {
        ParPartyName partyName = new ParPartyName();

        OznaceniTypTypA typ = oznaceniTyp.getTyp();
        if (typ == null) {
            if (isPreferred) {
                typ = OznaceniTypTypA.JMÉNO_ÚŘEDNÍ; // TODO jen pro předváděčku
//                throw new IllegalStateException("Prázdný typ preferovaného označení.");
            } else {
                return null;
            }
        }

        String partyNameFormTypeName = typ.value();
        ParPartyNameFormType parPartyNameFormType = partyNameFormTypeRepository.findByName(partyNameFormTypeName);
        if (parPartyNameFormType == null) {
            if (isPreferred) {
                partyNameFormTypeRepository.findByName(OznaceniTypTypA.JMÉNO_ÚŘEDNÍ.value()); // TODO jen pro předváděčku
//                throw new IllegalStateException("Neznámý název typu formy jména ");
            } else {
                return null;
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

    public ParParty createParty(final RegRecord regRecord, final Map<EntityValueType, List<Object>> valueMap, final boolean isOriginator) {
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
            default:
                logger.info("Import rejstříku s typem " + trida);
            case OSOBA_BYTOST:
                parParty = new ParPerson();
                parPartyType = partyTypeRepository.findPartyTypeByCode(PartyType.PERSON.getCode());
                break;
        }

        parParty.setPartyType(parPartyType);
        parParty.setRecord(regRecord);
        parParty.setOriginator(isOriginator);

        fillParty(parParty, valueMap);

        return parParty;
    }

    public Map<EntityValueType, List<Object>> convertToMap(final EntitaTyp entitaTyp) {
        Assert.assertNotNull(entitaTyp);
        Assert.assertNotNull(entitaTyp.getContent());

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

    private List<PopisTyp> getPopisTyp(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.POPIS);
    }

    private List<ZdrojTyp> getZdrojTyp(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.ZDROJ_INFORMACI);
    }

    private List<TitulTyp> getTitul(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.TITUL);
    }

    private List<IdentifikaceTyp> getIdentifikace(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.IDENTIFIKACE);
    }

    private List<OznaceniTyp> getVariantniOznaceni(final Map<EntityValueType, List<Object>> valueMap) {
        return getValueList(valueMap, EntityValueType.VARIANTNI_OZNACENI);
    }

    private OznaceniTyp getPreferovaneOznaceni(final Map<EntityValueType, List<Object>> valueMap) {
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

    private PodtridaTyp getPodTrida(final Map<EntityValueType, List<Object>> valueMap) {
        return getValue(valueMap, EntityValueType.PODTRIDA);
    }

    private TridaTyp getTrida(final Map<EntityValueType, List<Object>> valueMap) {
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
        if (!createDetailDefaultResource.exists()) {
            createDetailResource = null;
            return;
        }

        File dirRules = new File(interpiDir);
        if (!dirRules.exists()) {
            dirRules.mkdir();
        }

        File createTransformationFile = new File(FilenameUtils.concat(interpiDir, CREATE_TRANSFORMATION_FILE));

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
            throw new IllegalStateException("Nepodařilo se vytvořit soubor " + createTransformationFile.getAbsolutePath());
        }

        createDetailResource = new PathResource(createTransformationFile.toPath());
    }
}
