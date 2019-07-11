package cz.tacr.elza.interpi.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.enums.InterpiClass;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.InterpiEntityMappingVO;
import cz.tacr.elza.controller.vo.InterpiMappingVO;
import cz.tacr.elza.controller.vo.InterpiRelationMappingVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParInterpiMapping;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.interpi.service.InterpiSessionHolder.InterpiEntitySession;
import cz.tacr.elza.interpi.service.pqf.AttributeType;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.service.vo.InterpiEntity;
import cz.tacr.elza.interpi.service.vo.MappingVO;
import cz.tacr.elza.interpi.service.vo.PairedRecordVO;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorSouvTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorSouvTypA;
import cz.tacr.elza.interpi.ws.wo.SouvisejiciMinTyp;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.InterpiMappingRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.GroovyScriptService;

/**
 * Služba pro práci s INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2016
 */
@Service
public class InterpiService {

    public static final String EID_TYPE_CODE = "INTERPI";

    /** Oddělovač v klíči pro unikátnost mapování. */
    private static final String DELIMITER = "-%-%-";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClientFactoryVO factoryVO;

    @Autowired
    private InterpiSessionHolder interpiSessionHolder;

    @Autowired
    private InterpiFactory interpiFactory;

    @Autowired
    private InterpiClient interpiClient;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApStateRepository apStateRepository;

    @Autowired
    private InterpiMappingRepository interpiMappingRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private RelationTypeRoleTypeRepository relationTypeRoleTypeRepository;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private ApExternalIdRepository apEidRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Vyhledá záznamy v INTERPI.
     *
     * @param isParty příznak zda se mají hledat osoby nebo rejstříky
     * @param conditions seznam podmínek
     * @param count počet záznamů které se mají vrátit
     * @param systemId id externího systému
     *
     * @return seznam nalezených záznamů
     */
    public List<ExternalRecordVO> findRecords(final boolean isParty,
            final List<ConditionVO> conditions,
            final Integer count,
            final Integer systemId) {
        Assert.notNull(systemId, "Identifikátor systému musí být vyplněn");
        String interpiRecordId = findIdCondition(conditions);

        ApExternalSystem apExternalSystem = apExternalSystemRepository.findOne(systemId);
        List<EntitaTyp> records;
        if (StringUtils.isNotBlank(interpiRecordId)) {
            EntitaTyp entitaTyp = getRecordById(interpiRecordId, apExternalSystem);

            records = Collections.singletonList(entitaTyp);
        } else {
            String query = interpiFactory.createSearchQuery(conditions, isParty);
            if (StringUtils.isBlank(query)) {
                return Collections.emptyList();
            }

            records = interpiClient.findRecords(query, count, apExternalSystem);
        }

        Map<String, ExternalRecordVO> result = convertSearchResults(records, false);
        if (!result.isEmpty()) {
            matchWithExistingRecords(result);
        }

        return new ArrayList<>(result.values());
    }

    /**
     * Zjistí zda je v podmínkách podmínka na id. Když ano tak ho vrátí.
     *
     * @param conditions podmínky
     *
     * @return id pokud je v podmínkách
     */
    private String findIdCondition(final List<ConditionVO> conditions) {
        if (conditions == null) {
            return null;
        }

        String id = null;
        for (ConditionVO condition : conditions) {
            if (condition.getAttType() == AttributeType.ID) {
                id = condition.getValue();
                break;
            }
        }

        return id;
    }

    /**
     * Převod záznamů z INTERPI.
     *
     * @param records INTERPI záznamy
     * @param generateVariantNames příznak zda se mají generovat variantní jména
     *
     * @return mapa externí id rejstříku -> převedený záznam
     */
    private Map<String, ExternalRecordVO> convertSearchResults(final List<EntitaTyp> records, final boolean generateVariantNames) {
        return groovyScriptService.convertListToExternalRecordVO(records, generateVariantNames, interpiFactory).
                stream().
                collect(Collectors.toMap(ExternalRecordVO::getRecordId, Function.identity()));
    }

    /**
     * Načte konkrétní záznam podle externího id.
     *
     * @param interpiRecordId id v externím systému
     *
     * @return požadovaný záznam
     */
    private EntitaTyp getRecordById(final String interpiRecordId, final ApExternalSystem apExternalSystem) {
        Assert.notNull(interpiRecordId, "Identifikátor systému interpi musí být vyplněn");
        Assert.notNull(apExternalSystem, "Musí být vyplněn externí systém");

        return interpiClient.findOneRecord(interpiRecordId, apExternalSystem);
    }

    /**
     * Načte vztahy a jejich mapování.
     *
     * @param interpiRecordId id záznamu v INTERPI
     * @param systemId id systému
     * @param scopeId
     *
     * @return vztahy záznamu a jejich mapování
     */
    public InterpiMappingVO findInterpiRecordRelations(final String interpiRecordId, final Integer systemId, final Integer scopeId) {
        Assert.notNull(interpiRecordId, "Identifikátor systému interpi musí být vyplněn");
        Assert.notNull(systemId, "Identifikátor systému musí být vyplněn");
        Assert.notNull(scopeId, "Identifikátor scope musí být vyplněn");

        ApExternalSystem apExternalSystem = apExternalSystemRepository.findOne(systemId);
        EntitaTyp entitaTyp = interpiClient.findOneRecord(interpiRecordId, apExternalSystem);
        InterpiEntitySession interpiEntitySession = interpiSessionHolder.getInterpiEntitySession();
        interpiEntitySession.setEntitaTyp(entitaTyp);

        ApScope apScope = scopeRepository.findOne(scopeId);
        InterpiEntity interpiEntity = new InterpiEntity(entitaTyp);
        final ExternalRecordVO externalRecordVO = groovyScriptService.convertToExternalRecordVO(entitaTyp, false, interpiFactory);

        ApType apType = interpiFactory.getApType(interpiEntity);
        ParPartyType partyType = apType.getPartyType();
        if (partyType == null) {
            throw new BusinessException("Vztahy lze mapovat jen pro osoby.", BaseCode.PROPERTY_IS_INVALID).set("property", "partyType");
        }

        List<InterpiRelationMappingVO> mappings = interpiFactory.getRelations(interpiEntity, apExternalSystem, apScope);

        // TODO: preparecovat - JPA query nehleda entity v persistent context
        List<ParInterpiMapping> interpiMappings = interpiMappingRepository.findAll(); // načtení do hibernate cache
        for (InterpiRelationMappingVO relationMappingVO : mappings) {
            InterpiClass interpiClass = relationMappingVO.getInterpiClass();
            String interpiRelationType = relationMappingVO.getInterpiRelationType();

            List<InterpiEntityMappingVO> entities = relationMappingVO.getEntities();
            if (CollectionUtils.isNotEmpty(entities)) {
                for (InterpiEntityMappingVO entityMappingVO : entities) {
                    String interpiRoleType = entityMappingVO.getInterpiRoleType();

                    ParInterpiMapping mapping = interpiMappingRepository.findByInterpiClassAndInterpiRelationTypeAndInterpiRoleType(
                            interpiClass, interpiRelationType, interpiRoleType);

                    if (mapping != null) {
                        entityMappingVO.setId(mapping.getInterpiMappingId());
                        entityMappingVO.setRelationRoleTypeId(mapping.getRelationRoleType().getRoleTypeId());
                        relationMappingVO.setRelationTypeId(mapping.getRelationType().getRelationTypeId());
                    }
                }
            } else {
                ParInterpiMapping mapping = interpiMappingRepository.findByInterpiClassAndInterpiRelationTypeAndInterpiRoleType(
                        interpiClass, interpiRelationType, null);

                if (mapping != null) {
                    relationMappingVO.setRelationTypeId(mapping.getRelationType().getRelationTypeId());
                    relationMappingVO.setId(mapping.getInterpiMappingId());
                }
            }

        }

        return new InterpiMappingVO(partyType.getPartyTypeId(), mappings, externalRecordVO);
    }

    /**
     * Import rejstříku. Založí nebo aktualizuje rejstřík.
     *
     * @param recordId id rejstříku, pokud je null vytvoří se nový záznam, jinak se aktualizuje
     * @param interpiRecordId id záznamu v INTERPI
     * @param scopeId id scope
     * @param systemId id systému
     * @param mappings mapování vztahů
     *
     * @return nový/aktualizovaný rejstřík
     */
    public ApAccessPoint importRecord(final Integer recordId, final String interpiRecordId, final Integer scopeId, final Integer systemId, final boolean isOriginator,
                                 final List<InterpiRelationMappingVO> mappings) {
        Assert.notNull(interpiRecordId, "Identifikátor systému interpi musí být vyplněn");
        Assert.notNull(scopeId, "Identifikátor scope musí být vyplněn");
        Assert.notNull(systemId, "Identifikátor systému musí být vyplněn");

        logger.info("Import záznamu s identifikátorem " + interpiRecordId + " z interpi.");

        ApExternalSystem apExternalSystem = apExternalSystemRepository.findOne(systemId);
        ApScope apScope = scopeRepository.findOne(scopeId);

        ApState originalRecord;
        if (recordId == null) {
            ApExternalIdType eidType = staticDataService.getData().getApEidTypeByCode(EID_TYPE_CODE);
            ApState apState = apStateRepository.getActiveByExternalIdAndScope(interpiRecordId, eidType, apScope);
            if (apState != null) {
                throw new BusinessException("Záznam již existuje", ExternalCode.ALREADY_IMPORTED)
                        .set("id", interpiRecordId).set("scope", apScope.getName());
            }
            originalRecord = null;
        } else {
            ApAccessPoint accessPoint = accessPointRepository.findOne(recordId);
            originalRecord = apStateRepository.findLastByAccessPoint(accessPoint);
        }

        InterpiEntitySession interpiEntitySession = interpiSessionHolder.getInterpiEntitySession();
        EntitaTyp entitaTyp = interpiEntitySession.getEntitaTyp();
        if (entitaTyp == null) {
            entitaTyp = interpiClient.findOneRecord(interpiRecordId, apExternalSystem);
        }
        InterpiEntity interpiEntity = new InterpiEntity(entitaTyp);

        ApAccessPoint result;
        if (interpiFactory.isParty(interpiEntity)) {
            List<MappingVO> updatedMappings = processMappings(mappings);
            result = interpiFactory.importParty(interpiEntity, originalRecord, interpiRecordId, isOriginator, apScope, apExternalSystem, updatedMappings);
        } else {
            result = interpiFactory.importRecord(entitaTyp, interpiRecordId, apScope, apExternalSystem);
        }

        if (interpiEntity.getHierarchickaStruktura().size() > 0) {
            final SouvisejiciMinTyp rodicEntita = interpiEntity.getHierarchickaStruktura().get(interpiEntity.getHierarchickaStruktura().size() - 1).getSouvisejiciEntita();
            IdentifikatorSouvTyp parentRecordExtId = null;

            for (IdentifikatorSouvTyp identifikator : rodicEntita.getIdentifikator()) {
                if (IdentifikatorSouvTypA.INTERPI.equals(identifikator.getTyp())) {
                    parentRecordExtId = identifikator;
                    break;
                }
            }

            if (parentRecordExtId == null) {
                throw new SystemException("Při importu hierarchického hesla nebyl nalezen interpi idetifikátor rodiče.");
            }
        }

        interpiEntitySession.clear();

        return result;
    }

    /**
     * Zpracuje změny v nastavení mapování a donačte výchozí(nezměněná) mapování.
     *
     * @param mappings mapování z klienta
     *
     * @return nová a výchozí mapování
     */
    private List<MappingVO> processMappings(final List<InterpiRelationMappingVO> mappings) {
        if (CollectionUtils.isEmpty(mappings)) {
            return getDefaultMappings();
        }

        relationTypeRepository.findAll(); // načtení do hibernate cache
        relationRoleTypeRepository.findAll(); // načtení do hibernate cache

        Set<String> interpiKeys = new HashSet<>();
        List<MappingVO> mappingsToUse = new LinkedList<>();
        Map<String, ParInterpiMapping> mappingsToSave = new HashMap<>();
        for (InterpiRelationMappingVO relationMappingVO : mappings) {
            if (!relationMappingVO.getImportRelation()) {
                continue;
            }
            List<InterpiEntityMappingVO> entities = relationMappingVO.getEntities();
            if (CollectionUtils.isEmpty(entities)) {
                MappingVO mappingVO = createMappingVO(relationMappingVO, null);
                mappingsToUse.add(mappingVO);

                if (relationMappingVO.getSave() && relationMappingVO.getImportRelation()) {
                    String key = createMappingKey(relationMappingVO, null);
                    ParInterpiMapping interpiMapping = createParInterpiMapping(relationMappingVO, null);
                    mappingsToSave.put(key, interpiMapping);
                }
            } else {
                for (InterpiEntityMappingVO entityMappingVO : entities) {
                    MappingVO mappingVO = createMappingVO(relationMappingVO, entityMappingVO);
                    mappingsToUse.add(mappingVO);

                    if (!isValidRelationRoleTypeCombination(mappingVO)) {
                        continue;
                    }

                    String interpiKey = createInterpiKey(relationMappingVO, entityMappingVO);
                    if (relationMappingVO.getSave() && entityMappingVO.getSave() && relationMappingVO.getImportRelation() && entityMappingVO.getImportEntity() &&
                            !interpiKeys.contains(interpiKey)) {
                        String key = createMappingKey(relationMappingVO, entityMappingVO);
                        ParInterpiMapping interpiMapping = createParInterpiMapping(relationMappingVO, entityMappingVO);
                        mappingsToSave.put(key, interpiMapping);

                        interpiKeys.add(interpiKey);
                    }
                }
            }
        }

        saveMappings(mappingsToSave.values());

        return mappingsToUse;
    }

    /**
     * Uloží nebo aktualizuje mapování.
     *
     * @param mappingsToSave seznam mapování
     */
    private void saveMappings(final Collection<ParInterpiMapping> mappingsToSave) {

        for (ParInterpiMapping parInterpiMapping : mappingsToSave) {
            List<ParInterpiMapping> existingMappings = interpiMappingRepository.findByInterpiRelationType(parInterpiMapping.getInterpiRelationType());
            if (existingMappings.isEmpty()) {
                interpiMappingRepository.save(parInterpiMapping);
            } else {
                boolean sameRelations = true; // mají všechny načtené záznamy stejý typ vztahu jako ukládáný záznam?
                Integer relationId = parInterpiMapping.getRelationType().getRelationTypeId();
                for (ParInterpiMapping existingMapping : existingMappings) {
                    if (sameRelations && !relationId.equals(existingMapping.getRelationType().getRelationTypeId())) {
                        sameRelations = false;
                        break;
                    }
                }

                if (sameRelations) {
                    interpiMappingRepository.save(parInterpiMapping);
                } else {
                    existingMappings.remove(parInterpiMapping);
                    interpiMappingRepository.delete(existingMappings);
                    interpiMappingRepository.save(parInterpiMapping);
                }
            }
        }
    }

    /** Zkontroluje zda pro typ vztahu a typ role existuje vazba. */
    private boolean isValidRelationRoleTypeCombination(final MappingVO mappingVO) {
        ParRelationTypeRoleType parRelationTypeRoleType = new ParRelationTypeRoleType();
        parRelationTypeRoleType.setRelationType(mappingVO.getParRelationType());
        parRelationTypeRoleType.setRoleType(mappingVO.getParRelationRoleType());

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withMatcher("relationType", GenericPropertyMatchers.exact())
                .withMatcher("roleType", GenericPropertyMatchers.exact());

        Example<ParRelationTypeRoleType> example = Example.of(parRelationTypeRoleType, matcher);
        return relationTypeRoleTypeRepository.exists(example);
    }


    /**
     * @return klíč identifikující interpi vazby
     */
    private String createInterpiKey(final InterpiRelationMappingVO relationMappingVO,
            final InterpiEntityMappingVO entityMappingVO) {
        Assert.notNull(relationMappingVO, "Mapovací klíč musí být vyplněn");
        Assert.notNull(entityMappingVO, "Mapovací entita musí být vyplněna");

        // klíč je ve tvaru interpiRelationType-%-%-interpiClass-%-%-interpiRoleType
        return new StringBuilder(StringUtils.trimToEmpty(relationMappingVO.getInterpiRelationType())).
                append(DELIMITER).
                append(relationMappingVO.getInterpiClass()).
                append(DELIMITER).
                append(entityMappingVO.getInterpiRoleType()).
                toString();
    }


    /**
     * @return klíč identifikující mapování vztahu a role
     */
    private String createMappingKey(final InterpiRelationMappingVO relationMappingVO,
            final InterpiEntityMappingVO entityMappingVO) {
        Assert.notNull(relationMappingVO, "Mapovací klíč musí být vyplněn");

        // klíč je ve tvaru interpiRelationType-%-%-elzaRelationTypeId-%-%-interpiRoleType-%-%-elzaRoleTypeId
        StringBuilder sb = new StringBuilder(StringUtils.trimToEmpty(relationMappingVO.getInterpiRelationType())).
                append(DELIMITER).
                append(relationMappingVO.getRelationTypeId());

        if (entityMappingVO != null) {
            sb.append(DELIMITER).
                append(entityMappingVO.getInterpiRoleType()).
                append(DELIMITER).
                append(entityMappingVO.getRelationRoleTypeId());
        }
        return sb.toString();
    }

    /**
     * Převede hierarchickou reprezentaci mapování z klienta na plochou strukturu pro import.
     *
     * @param relationMappingVO informace o vztahu
     * @param entityMappingVO informace o entitě ve vztahu, může být null
     *
     * @return plochá reprezentace mapování
     */
    private MappingVO createMappingVO(final InterpiRelationMappingVO relationMappingVO,
            final InterpiEntityMappingVO entityMappingVO) {
        MappingVO mappingVO = new MappingVO();

        if (entityMappingVO != null) {
            mappingVO.setInterpiRoleType(entityMappingVO.getInterpiRoleType());
            if (entityMappingVO.getImportEntity()) {
                mappingVO.setParRelationRoleType(relationRoleTypeRepository.findOne(entityMappingVO.getRelationRoleTypeId()));
            }
            mappingVO.setImportRelation(entityMappingVO.getImportEntity());
            mappingVO.setInterpiId(entityMappingVO.getInterpiId());
        } else {
            mappingVO.setImportRelation(relationMappingVO.getImportRelation());
        }

        mappingVO.setInterpiClass(relationMappingVO.getInterpiClass());
        mappingVO.setInterpiRelationType(relationMappingVO.getInterpiRelationType());
        mappingVO.setParRelationType(relationTypeRepository.findOne(relationMappingVO.getRelationTypeId()));

        return mappingVO;
    }

    /**
     * Převede hierarchickou reprezentaci mapování z klienta na plochou strukturu pro uložení do db.
     *
     * @param relationMappingVO informace o vztahu
     * @param entityMappingVO informace o entitě ve vztahu, může být null
     *
     * @return plochá reprezentace mapování
     */
    private ParInterpiMapping createParInterpiMapping(final InterpiRelationMappingVO relationMappingVO,
            final InterpiEntityMappingVO entityMappingVO) {
        ParInterpiMapping interpiMapping = new ParInterpiMapping();

        if (entityMappingVO != null) {
            interpiMapping.setInterpiMappingId(entityMappingVO.getId());
            interpiMapping.setInterpiRoleType(entityMappingVO.getInterpiRoleType());
            interpiMapping.setRelationRoleType(relationRoleTypeRepository.findOne(entityMappingVO.getRelationRoleTypeId()));
        } else {
            interpiMapping.setInterpiMappingId(relationMappingVO.getId());
        }

        interpiMapping.setInterpiClass(relationMappingVO.getInterpiClass());
        interpiMapping.setInterpiRelationType(relationMappingVO.getInterpiRelationType());
        interpiMapping.setRelationType(relationTypeRepository.findOne(relationMappingVO.getRelationTypeId()));

        return interpiMapping;
    }

    /**
     * Načtení mapování z db.
     */
    private List<MappingVO> getDefaultMappings() { //Předělat aby se tam dostávalo interpi id entity
        List<ParInterpiMapping> interpiMappings = interpiMappingRepository.findAll();
        List<MappingVO> defaultMappings = new ArrayList<>(interpiMappings.size());
        for (ParInterpiMapping parInterpiMapping : interpiMappings) {
            MappingVO mappingVO = new MappingVO();

            mappingVO.setImportRelation(true);
            mappingVO.setInterpiClass(parInterpiMapping.getInterpiClass());
            mappingVO.setInterpiRelationType(parInterpiMapping.getInterpiRelationType());
            mappingVO.setInterpiRoleType(parInterpiMapping.getInterpiRoleType());
            mappingVO.setParRelationRoleType(parInterpiMapping.getRelationRoleType());
            mappingVO.setParRelationType(parInterpiMapping.getRelationType());

            defaultMappings.add(mappingVO);
        }

        return defaultMappings;
    }

    /**
     * Dohledání existujících rejstříkových hesel pro vyhledané záznamy.
     *
     * @param externalRecords nalezené záznamy pro které se mají najít existující hesla
     */
    private void matchWithExistingRecords(final Map<String, ExternalRecordVO> externalRecords) {
        ApExternalIdType eidType = staticDataService.getData().getApEidTypeByCode(EID_TYPE_CODE);
        if (eidType == null) {
            throw new ObjectNotFoundException("Nebyl nalezen typ externího identifikátoru", BaseCode.ID_NOT_EXIST)
                    .set("externalIdTypeCode", EID_TYPE_CODE);
        }
        List<ApExternalIdInfo> eidInfoList = apEidRepository
                .findInfoByExternalIdTypeIdAndValuesIn(eidType.getExternalIdTypeId(), externalRecords.keySet());

        StaticDataProvider staticData = staticDataService.getData();
        Map<Integer, ApScopeVO> convertedScopes = new HashMap<>();
        for (ApExternalIdInfo eidInfo : eidInfoList) {
            ExternalRecordVO recordVO = externalRecords.get(eidInfo.getValue());

            Integer apScopeId = eidInfo.getAccessPoint().getScopeId();
            ApScopeVO apScopeVO = convertedScopes.get(apScopeId);
            if (apScopeVO == null) {
                ApScope apScope = scopeRepository.findOne(apScopeId);
                apScopeVO = ApScopeVO.newInstance(apScope, staticData);
                convertedScopes.put(apScopeId, apScopeVO);
            }
            
            Integer apId = eidInfo.getAccessPoint().getAccessPointId();
            Integer partyId = null;
            ParParty existingParty = partyRepository.findParPartyByAccessPointId(apId);
            if (existingParty != null) {
                partyId = existingParty.getPartyId();
            }
            PairedRecordVO pairedRecordVO = new PairedRecordVO(apScopeVO, apId, partyId);
            recordVO.addPairedRecord(pairedRecordVO);
        }

        /* TODO: po testech odebrat - přepracováno v rámci 0.16
        Map<String, List<ApAccessPointData>> externalIdToApRecordsMap = apDataList.stream().collect(Collectors.groupingBy(o -> o.getExternalId().getValue()));

        for (String externalId : externalRecords.keySet()) {
            List<ApAccessPointData> sameRecords = externalIdToApRecordsMap.get(externalId);
            if (sameRecords != null) {
                for (ApAccessPointData existingRecord : sameRecords) {
                    ExternalRecordVO recordVO = externalRecords.get(externalId);

                    ApScope apScope = existingRecord.getAccessPoint().getScope();
                    ApScopeVO apScopeVO = factoryVO.getOrCreateVo(apScope.getScopeId(), apScope, convertedScopes, ApScopeVO.class);

                    Integer recordId = existingRecord.getAccessPointId();
                    Integer partyId = null;
                    ParParty existingParty = partyService.findParPartyByAccessPoint(existingRecord.getAccessPoint());
                    if (existingParty != null) {
                        partyId = existingParty.getPartyId();
                    }
                    PairedRecordVO pairedRecordVO = new PairedRecordVO(apScopeVO, recordId, partyId);
                    recordVO.addPairedRecord(pairedRecordVO);
                }
            }
        } */
    }

    /**
     * Odstraní interpi mapování které mají neplatnou kombinaci vztahů a rolí.
     */
    public void deleteInvalidMappings() {
        List<Integer> invalidMappingIds = interpiMappingRepository.findInvalidMappingIds();
        invalidMappingIds.forEach(id -> interpiMappingRepository.delete(id));
    }
}
