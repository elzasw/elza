package cz.tacr.elza.interpi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.service.vo.PairedRecordVO;
import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.SetTyp;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.WSUtils;
import cz.tacr.elza.utils.XmlUtils;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2016
 */
@Service
public class InterpiService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Stránkování. */
    private static final String FROM = "1";
    private static final String TO = "500";
    private static final Integer TO_INT = Integer.valueOf(TO);

    @Autowired
    private ClientFactoryVO factoryVO;

    @Autowired
    private PartyService partyService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private InterpiFactory interpiFactory;

    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

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
        Assert.assertNotNull(systemId);

        String query = interpiFactory.createSearchQuery(conditions, isParty);
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        Integer maxCount = count == null ? TO_INT : count;
        RegExternalSystem regExternalSystem = getExternalSystem(systemId);

        WssoapSoap client = createClient(regExternalSystem);
        logger.info("Vyhledávání v interpi: " + query);
        String searchResult = client.findData(query, null, FROM, maxCount.toString(),
                regExternalSystem.getUsername(), regExternalSystem.getPassword());

        SetTyp setTyp = unmarshall(searchResult);

        Map<String, ExternalRecordVO> result = convertSearchResults(setTyp.getEntita(), maxCount, false);
        if (!result.isEmpty()) {
            matchWithExistingRecords(regExternalSystem, result);
        }

        return new ArrayList<>(result.values());
    }

    private Map<String, ExternalRecordVO> convertSearchResults(final List<EntitaTyp> entitaList, final Integer maxCount,
            final boolean generateVariantNames) {
        int searchResultCount = entitaList.size() > maxCount ? maxCount : entitaList.size();
        List<EntitaTyp> searchResults = new ArrayList<>(searchResultCount);
        for (EntitaTyp entitaTyp : entitaList) {
            searchResults.add(entitaTyp);
            if (searchResults.size() == searchResultCount) {
                break; // INTERPI neumí stránkovat
            }
        }

        List<ExternalRecordVO> recordVOList = interpiFactory.convertToExternalRecordVO(searchResults, generateVariantNames);
        Map<String, ExternalRecordVO> result = new HashMap<>();
        for (ExternalRecordVO externalRecordVO : recordVOList) {
            result.put(externalRecordVO.getRecordId(), externalRecordVO);
        }

        return result;
    }

    /**
     * Načte konkrétní záznam podle externího id.
     *
     * @param id id v externím systému
     * @param systemId id externího systému
     *
     * @return požadovaný záznam
     */
    public ExternalRecordVO getRecordById(final String id, final Integer systemId) {
        Assert.assertNotNull(id);
        Assert.assertNotNull(systemId);

        RegExternalSystem interpiSystem = getExternalSystem(systemId);
        EntitaTyp entitaTyp = findOneRecord(id, interpiSystem);

        ExternalRecordVO recordVO = interpiFactory.convertToExternalRecordVO(Collections.singletonList(entitaTyp), false).iterator().next();
        matchWithExistingRecords(interpiSystem, Collections.singletonMap(recordVO.getRecordId(), recordVO));

        return recordVO;
    }

    /**
     * Import rejstříku. Založí nebo aktualizuje rejstřík.
     *
     * @param recordId id rejstříku, pokud je null vytvoří se nový záznam, jinak se aktualizuje
     * @param interpiRecordId id záznamu v INTERPI
     * @param scopeId id scope
     * @param systemId id systému
     *
     * @return nový/aktualizovaný rejstřík
     */
    public RegRecord importRecord(final Integer recordId, final String interpiRecordId, final Integer scopeId, final Integer systemId, final boolean isOriginator) {
        Assert.assertNotNull(interpiRecordId);
        Assert.assertNotNull(scopeId);
        Assert.assertNotNull(systemId);

        logger.info("Import záznamu s identifikátorem " + interpiRecordId + " z interpi.");

        RegExternalSystem regExternalSystem = getExternalSystem(systemId);
        RegScope regScope = scopeRepository.findOne(scopeId);

        RegRecord originalRecord = null;
        if (recordId == null) {
            RegRecord regRecord = recordRepository.findRegRecordByExternalIdAndExternalSystemCodeAndScope(interpiRecordId,
                    regExternalSystem.getCode(), regScope);
            if (regRecord != null) {
                throw new IllegalStateException("Nelze naimportovat existující rejstříkové heslo. Heslo s externím identifikátorem "
                        + interpiRecordId + " již existuje ve třídě " + regScope.getName());
            }
        } else {
            originalRecord = recordRepository.findOne(recordId);
        }

        EntitaTyp entitaTyp = findOneRecord(interpiRecordId, regExternalSystem);
        Map<EntityValueType, List<Object>> valueMap = interpiFactory.convertToMap(entitaTyp);

        RegRecord result;
        if (interpiFactory.isParty(valueMap)) {
            result = importParty(valueMap, originalRecord, interpiRecordId, isOriginator, regScope, regExternalSystem);
        } else {
            result = importRecord(entitaTyp, originalRecord, interpiRecordId, regScope, regExternalSystem);
        }

        return result;
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
    private RegRecord importRecord(final EntitaTyp entitaTyp, final RegRecord originalRecord,
            final String interpiRecordId,  final RegScope regScope, final RegExternalSystem regExternalSystem) {
        RegRecord regRecord = interpiFactory.createRecord(entitaTyp, interpiRecordId, regExternalSystem, regScope, true);
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
    private RegRecord importParty(final Map<EntityValueType, List<Object>> valueMap, final RegRecord originalRecord,
            final String interpiRecordId, final boolean isOriginator, final RegScope regScope,
            final RegExternalSystem regExternalSystem) {
        RegRecord regRecord = interpiFactory.createPartyRecord(valueMap, interpiRecordId, regExternalSystem, regScope);
        ParParty newParty = interpiFactory.createParty(regRecord, valueMap, isOriginator);

        if (originalRecord != null) {
            regRecord.setRecordId(originalRecord.getRecordId());
            regRecord.setVersion(originalRecord.getVersion());

            ParParty originalParty = partyService.findParPartyByRecord(originalRecord);
            newParty.setPartyId(originalParty.getPartyId());
            newParty.setVersion(originalParty.getVersion());

            List<ParRelation> relations = originalParty.getRelations();
            if (CollectionUtils.isNotEmpty(relations)) {
                for (ParRelation relation : relations) {
                    partyService.deleteRelation(relation);
                }
            }
        }

        ParParty parParty = partyService.saveParty(newParty);
        List<ParRelation> relations = parParty.getRelations();
        if (CollectionUtils.isNotEmpty(relations)) {
            for (ParRelation relation : relations) {
                partyService.saveRelation(relation, null);
            }
        }

        return parParty.getRecord();
    }

    private EntitaTyp findOneRecord(final String id, final RegExternalSystem regExternalSystem) {
        WssoapSoap client = createClient(regExternalSystem);

        logger.info("Načítání záznamu s identifikátorem " + id + " z interpi.");
        String oneRecord = client.getOneRecord(id, regExternalSystem.getUsername(), regExternalSystem.getPassword());
        SetTyp setTyp = unmarshall(oneRecord);

        if (setTyp.getEntita().isEmpty()) {
            throw new IllegalStateException("Záznam s identifikátorem " + id + " nebyl nalezen v systému " + regExternalSystem);
        }

        return setTyp.getEntita().iterator().next();
    }

    private WssoapSoap createClient(final RegExternalSystem regExternalSystem) {
        return WSUtils.createClient(regExternalSystem.getUrl(), WssoapSoap.class);
    }

    private RegExternalSystem getExternalSystem(final Integer systemId) {
        return regExternalSystemRepository.findOne(systemId);
    }

    private SetTyp unmarshall(final String oneRecord) {
        return XmlUtils.unmarshallDataWithIntrospector(oneRecord, SetTyp.class);
    }
}
