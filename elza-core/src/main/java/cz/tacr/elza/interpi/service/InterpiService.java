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
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.service.vo.PairedRecordVO;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.repository.InterpiMappingRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.ScopeRepository;

/**
 * Služba pro práci s INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2016
 */
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

        String query = interpiFactory.createSearchQuery(conditions, isParty);
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        RegExternalSystem regExternalSystem = regExternalSystemRepository.findOne(systemId);
        List<EntitaTyp> records = interpiClient.findRecords(query, count, regExternalSystem);

        Map<String, ExternalRecordVO> result = convertSearchResults(records, false);
        if (!result.isEmpty()) {
            matchWithExistingRecords(regExternalSystem, result);
        }

        return new ArrayList<>(result.values());
    }

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
     * @param systemId id externího systému
     *
     * @return požadovaný záznam
     */
    public ExternalRecordVO getRecordById(final String interpiRecordId, final Integer systemId) {
        Assert.notNull(interpiRecordId);
        Assert.notNull(systemId);

        RegExternalSystem interpiSystem = regExternalSystemRepository.findOne(systemId);
        EntitaTyp entitaTyp = interpiClient.findOneRecord(interpiRecordId, interpiSystem);

        ExternalRecordVO recordVO = interpiFactory.convertToExternalRecordVO(Collections.singletonList(entitaTyp), false).iterator().next();
        matchWithExistingRecords(interpiSystem, Collections.singletonMap(recordVO.getRecordId(), recordVO));

        return recordVO;
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

        List<InterpiRelationMappingVO> updatedMappings = processMappings(mappings);

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

        EntitaTyp entitaTyp = interpiClient.findOneRecord(interpiRecordId, regExternalSystem);
        Map<EntityValueType, List<Object>> valueMap = interpiFactory.convertToMap(entitaTyp);

        RegRecord result;
        if (interpiFactory.isParty(valueMap)) {
            result = interpiFactory.importParty(valueMap, originalRecord, interpiRecordId, isOriginator, regScope, regExternalSystem, updatedMappings);
        } else {
            result = interpiFactory.importRecord(entitaTyp, originalRecord, interpiRecordId, regScope, regExternalSystem);
        }

        return result;
    }

    /**
     * Zpracuje změny v nastavení mapování a donačte výchozí(nezměněná) mapování.
     *
     * @param mappings mapování z klienta
     *
     * @return nová a výchozí mapování
     */
    private List<InterpiRelationMappingVO> processMappings(final List<InterpiRelationMappingVO> mappings) {
        if (true /*CollectionUtils.isEmpty(mappings)*/) { // TODO upravit až budou chodit z klienta
            return getDefaultMappings();
        }
      //TODO dodělat zpracování změněných mapování
//        if (CollectionUtils.isEmpty(mappings)) {
//            List<ParInterpiMapping> interpiMappings = interpiMappingRepository.findAll();
//            mappings = new LinkedList<>();
//            for (ParInterpiMapping parInterpiMapping : interpiMappings) {
//
//            }
//        }
        return null;
    }

    private List<InterpiRelationMappingVO> getDefaultMappings() {
        List<InterpiRelationMappingVO> updatedMappings = new LinkedList<>();

        List<ParInterpiMapping> interpiMappings = interpiMappingRepository.findAll();
        Map<String, List<ParInterpiMapping>> mappingsGroupedByInterpiRelationType = new HashMap<>();
        for (ParInterpiMapping parInterpiMapping : interpiMappings) {
            String interpiRelationType = parInterpiMapping.getInterpiRelationType();
            List<ParInterpiMapping> relationMappings = mappingsGroupedByInterpiRelationType.get(interpiRelationType);
            if (relationMappings == null) {
                relationMappings = new LinkedList<>();
                mappingsGroupedByInterpiRelationType.put(interpiRelationType, relationMappings);
            }

            relationMappings.add(parInterpiMapping);
        }

        for (List<ParInterpiMapping> mappingsWithSameInterpiRelationType : mappingsGroupedByInterpiRelationType.values()) {
            InterpiRelationMappingVO relationItemVO = null;

            Map<Integer, List<ParInterpiMapping>> mappingsGroupedByIds = mappingsWithSameInterpiRelationType.stream().
                    collect(Collectors.groupingBy(ParInterpiMapping::getInterpiMappingId));
            for (List<ParInterpiMapping> parInterpiMappings : mappingsGroupedByIds.values()) {
                for (ParInterpiMapping parInterpiMapping : parInterpiMappings) {
                    if (relationItemVO == null) {
                        relationItemVO = new InterpiRelationMappingVO();

                        relationItemVO.setId(parInterpiMapping.getInterpiMappingId());
                        relationItemVO.setImportRelation(true);
                        relationItemVO.setInterpiClass(parInterpiMapping.getInterpiClass());
                        relationItemVO.setInterpiRelationType(parInterpiMapping.getInterpiRelationType());
                        relationItemVO.setRelationTypeId(parInterpiMapping.getRelationType().getRelationTypeId());

                        updatedMappings.add(relationItemVO);
                    }

                    if (StringUtils.isNotBlank(parInterpiMapping.getInterpiRoleType())) {
                        InterpiEntityMappingVO entityMappingVO = new InterpiEntityMappingVO();

                        entityMappingVO.setId(parInterpiMapping.getInterpiMappingId());
                        entityMappingVO.setImportRelation(true);
                        entityMappingVO.setInterpiRoleType(parInterpiMapping.getInterpiRoleType());
                        entityMappingVO.setRelationRoleTypeId(parInterpiMapping.getRelationRoleType().getRoleTypeId());

                        relationItemVO.addEntityMapping(entityMappingVO);
                    }

                }
            }
        }

        return updatedMappings;
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
}
