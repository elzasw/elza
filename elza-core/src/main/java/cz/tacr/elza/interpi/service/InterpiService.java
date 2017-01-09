package cz.tacr.elza.interpi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.InterpiClass;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.InterpiEntityMappingVO;
import cz.tacr.elza.controller.vo.InterpiMappingVO;
import cz.tacr.elza.controller.vo.InterpiRelationMappingVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.domain.ParInterpiMapping;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.interpi.service.pqf.AttributeType;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.service.vo.MappingVO;
import cz.tacr.elza.interpi.service.vo.PairedRecordVO;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.repository.InterpiMappingRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;

/**
 * Služba pro práci s INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2016
 */
@Configuration
@Service
public class InterpiService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClientFactoryVO factoryVO;

    @Autowired
    private InterpiFactory interpiFactory;

    @Autowired
    private InterpiClient interpiClient;

    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private InterpiMappingRepository interpiMappingRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

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
        Assert.notNull(systemId);

        String interpiRecordId = findIdCondition(conditions);

        RegExternalSystem regExternalSystem = regExternalSystemRepository.findOne(systemId);
        List<EntitaTyp> records;
        if (StringUtils.isNotBlank(interpiRecordId)) {
            EntitaTyp entitaTyp = getRecordById(interpiRecordId, regExternalSystem);

            records = Collections.singletonList(entitaTyp);
        } else {
            String query = interpiFactory.createSearchQuery(conditions, isParty);
            if (StringUtils.isBlank(query)) {
                return Collections.emptyList();
            }

            records = interpiClient.findRecords(query, count, regExternalSystem);
        }

        Map<String, ExternalRecordVO> result = convertSearchResults(records, false);
        if (!result.isEmpty()) {
            matchWithExistingRecords(regExternalSystem, result);
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
        List<ExternalRecordVO> recordVOList = interpiFactory.convertToExternalRecordVO(records, generateVariantNames);
        Map<String, ExternalRecordVO> result = new HashMap<>();
        for (ExternalRecordVO externalRecordVO : recordVOList) {
            result.put(externalRecordVO.getRecordId(), externalRecordVO);
        }

        return result;
    }

    /**
     * Načte konkrétní záznam podle externího id.
     *
     * @param interpiRecordId id v externím systému
     * @param interpiSystem externí systém
     *
     * @return požadovaný záznam
     */
    private EntitaTyp getRecordById(final String interpiRecordId, final RegExternalSystem regExternalSystem) {
        Assert.notNull(interpiRecordId);
        Assert.notNull(regExternalSystem);

        return interpiClient.findOneRecord(interpiRecordId, regExternalSystem);
    }

    /**
     * Načte vztahy a jejich mapování.
     *
     * @param interpiRecordId id záznamu v INTERPI
     * @param systemId id systému
     *
     * @return vztahy záznamu a jejich mapování
     */
    public InterpiMappingVO findInterpiRecordRelations(final String interpiRecordId, final Integer systemId) {
        Assert.notNull(interpiRecordId);
        Assert.notNull(systemId);

        RegExternalSystem regExternalSystem = regExternalSystemRepository.findOne(systemId);
        EntitaTyp entitaTyp = interpiClient.findOneRecord(interpiRecordId, regExternalSystem);
        getInterpiEntitySession().setEntitaTyp(entitaTyp);

        Map<EntityValueType, List<Object>> valueMap = interpiFactory.convertToMap(entitaTyp);

        RegRegisterType regRegisterType = interpiFactory.getRegisterType(valueMap);
        ParPartyType partyType = regRegisterType.getPartyType();
        if (partyType == null) {
            throw new IllegalStateException("Vztahy lze mapovat jen pro osoby.");
        }

        List<InterpiRelationMappingVO> mappings = interpiFactory.getRelations(valueMap);

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

        return new InterpiMappingVO(partyType.getPartyTypeId(), mappings);
    }

    /**
     * Import rejstříku. Založí nebo aktualizuje rejstřík.
     *
     * @param recordId id rejstříku, pokud je null vytvoří se nový záznam, jinak se aktualizuje
     * @param interpiRecordId id záznamu v INTERPI
     * @param scopeId id scope
     * @param systemId id systému
     * @param importRelations příznak zda se mají importovat i vztahy
     * @param mappings mapování vztahů
     *
     * @return nový/aktualizovaný rejstřík
     */
    public RegRecord importRecord(final Integer recordId, final String interpiRecordId, final Integer scopeId, final Integer systemId, final boolean isOriginator,
            final List<InterpiRelationMappingVO> mappings) {
        Assert.notNull(interpiRecordId);
        Assert.notNull(scopeId);
        Assert.notNull(systemId);

        logger.info("Import záznamu s identifikátorem " + interpiRecordId + " z interpi.");

        List<MappingVO> updatedMappings = processMappings(mappings);

        RegExternalSystem regExternalSystem = regExternalSystemRepository.findOne(systemId);
        RegScope regScope = scopeRepository.findOne(scopeId);

        RegRecord originalRecord = null;
        if (recordId == null) {
            RegRecord regRecord = recordRepository.findRegRecordByExternalIdAndExternalSystemCodeAndScope(interpiRecordId,
                    regExternalSystem.getCode(), regScope);
            if (regRecord != null) {
                throw new BusinessException(ExternalCode.ALREADY_IMPORTED).set("id", interpiRecordId).set("scope", regScope.getName());
            }
        } else {
            originalRecord = recordRepository.findOne(recordId);
        }

        EntitaTyp entitaTyp = getInterpiEntitySession().getEntitaTyp();
        if (entitaTyp == null) {
            entitaTyp = interpiClient.findOneRecord(interpiRecordId, regExternalSystem);
        }
        Map<EntityValueType, List<Object>> valueMap = interpiFactory.convertToMap(entitaTyp);

        RegRecord result;
        if (interpiFactory.isParty(valueMap)) {
            result = interpiFactory.importParty(valueMap, originalRecord, interpiRecordId, isOriginator, regScope, regExternalSystem, updatedMappings);
        } else {
            result = interpiFactory.importRecord(entitaTyp, originalRecord, interpiRecordId, regScope, regExternalSystem);
        }

        getInterpiEntitySession().setEntitaTyp(null);

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

        List<MappingVO> mappingsToUse = new LinkedList<>();
        List<ParInterpiMapping > mappingsToSave = new LinkedList<>();
        for (InterpiRelationMappingVO relationMappingVO : mappings) {
            List<InterpiEntityMappingVO> entities = relationMappingVO.getEntities();
            if (CollectionUtils.isEmpty(entities)) {
                MappingVO mappingVO = createMappingVO(relationMappingVO, null);
                ParInterpiMapping interpiMapping = createParInterpiMapping(relationMappingVO, null);

                if (relationMappingVO.getImportRelation()) {
                    mappingsToUse.add(mappingVO);
                }
                if (relationMappingVO.getSave()) {
                    mappingsToSave.add(interpiMapping);
                }
            } else {
                for (InterpiEntityMappingVO entityMappingVO : entities) {
                    MappingVO mappingVO = createMappingVO(relationMappingVO, entityMappingVO);
                    ParInterpiMapping interpiMapping = createParInterpiMapping(relationMappingVO, entityMappingVO);

                    if (relationMappingVO.getImportRelation()) {
                        mappingsToUse.add(mappingVO);
                    }
                    if (relationMappingVO.getSave()) {
                        mappingsToSave.add(interpiMapping);
                    }
                }
            }
        }

        interpiMappingRepository.save(mappingsToSave);

        return mappingsToUse;
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
            mappingVO.setParRelationRoleType(relationRoleTypeRepository.findOne(entityMappingVO.getRelationRoleTypeId()));
            mappingVO.setImportRelation(entityMappingVO.getImportEntity());
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
    private List<MappingVO> getDefaultMappings() {
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
     * @param regExternalSystem systém ze kterého jsou nalezená rejstříková hesla
     * @param externalRecords nalezené záznamy pro které se mají najít existující hesla
     */
    private void matchWithExistingRecords(final RegExternalSystem regExternalSystem, final Map<String, ExternalRecordVO> externalRecords) {
        List<RegRecord> regRecords = recordRepository.findRegRecordByExternalIdsAndExternalSystem(externalRecords.keySet(), regExternalSystem);

        Map<String, List<RegRecord>> externalIdToRegRecordsMap = regRecords.stream().collect(Collectors.groupingBy(RegRecord::getExternalId));

        Map<Integer, RegScopeVO> convertedScopes = new HashMap<>();
        for (String externalId : externalRecords.keySet()) {
            List<RegRecord> sameRecords = externalIdToRegRecordsMap.get(externalId);
            if (sameRecords != null) {
                for (RegRecord existingRecord : sameRecords) {
                    ExternalRecordVO recordVO = externalRecords.get(externalId);

                    RegScope regScope = existingRecord.getScope();
                    RegScopeVO regScopeVO = factoryVO.getOrCreateVo(regScope.getScopeId(), regScope, convertedScopes, RegScopeVO.class);

                    PairedRecordVO pairedRecordVO = new PairedRecordVO(regScopeVO, existingRecord.getRecordId());
                    recordVO.addPairedRecord(pairedRecordVO);
                }
            }
        }
    }

    /**
     * @return vrací session uživatele
     */
    @Bean
    @Scope("session")
    protected InterpiEntitySession getInterpiEntitySession() {
        return new InterpiEntitySession();
    }


    /**
     * Session uživatele s načtenou entitou z INTERPI.
     */
    @Component
    private static class InterpiEntitySession {

        private EntitaTyp entitaTyp;

        public EntitaTyp getEntitaTyp() {
            return entitaTyp;
        }

        public void setEntitaTyp(final EntitaTyp entitaTyp) {
            this.entitaTyp = entitaTyp;
        }
    }
}
