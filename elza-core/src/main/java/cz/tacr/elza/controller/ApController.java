package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ApRecordVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.ApRecordRepository;
import cz.tacr.elza.repository.ApVariantRecordRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.ApService;


/**
 * REST Kontrolér pro registry.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@RestController
@RequestMapping(value = "/api/registry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApRecordRepository apRecordRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private ApService apService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ApVariantRecordRepository variantRecordRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private InterpiService interpiService;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param apTypeId   IDčka typu záznamu, může být null
     * @param parentRecordId    id rodiče, pokud je null načtou se všechny záznamy, jinak potomci daného rejstříku
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @param itemSpecId   id specifikace
     * @param scopeId           id scope, pokud je vyplněn vrací se jen rejstříky s tímto scope
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
	@Transactional
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public FilteredResultVO<ApRecord> findRecord(@RequestParam(required = false) @Nullable final String search,
                                                 @RequestParam final Integer from,
                                                 @RequestParam final Integer count,
                                                 @RequestParam(required = false) @Nullable final Integer apTypeId,
                                                 @RequestParam(required = false) @Nullable final Integer parentRecordId,
                                                 @RequestParam(required = false) @Nullable final Integer versionId,
                                                 @RequestParam(required = false) @Nullable final Integer itemSpecId,
                                                 @RequestParam(required = false) @Nullable final Integer scopeId,
                                                 @RequestParam(required = false) @Nullable final Integer lastRecordNr,
                                                 @RequestParam(required = false, defaultValue = "true") @Nullable final Boolean excludeInvalid) {

        Set<Integer> apTypeIdTree = Collections.emptySet();

        if (itemSpecId != null && apTypeId != null) {
            throw new SystemException("Nelze použít specifikaci a typ rejstříku zároveň.", BaseCode.SYSTEM_ERROR);
        } else if (itemSpecId != null || apTypeId != null) {
            Set<Integer> apTypeIds = new HashSet<>();
            if (itemSpecId != null) {
                RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
                apTypeIds.addAll(itemSpecRegisterRepository.findIdsByItemSpecId(spec));
            } else {
                apTypeIds.add(apTypeId);
            }
            apTypeIdTree = apTypeRepository.findSubtreeIds(apTypeIds);
        }

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
            fund = version.getFund();
        }

        if(parentRecordId != null) {
            apRecordRepository.getOneCheckExist(parentRecordId);
        }

        final long foundRecordsCount = apService.findApRecordByTextAndTypeCount(search, apTypeIdTree,
                parentRecordId, fund, scopeId, excludeInvalid);

        List<ApRecord> foundRecords = apService.findApRecordByTextAndType(search, apTypeIdTree, from,
                count, parentRecordId, fund, scopeId, excludeInvalid);


        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(foundRecords);

        List<ApRecordVO> foundRecordVOList = factoryVo.createApRecords(foundRecords, recordIdPartyIdMap, true);

        for (ApRecordVO apRecordVO : foundRecordVOList) {
            factoryVo.fillApTypeNamesToParents(apRecordVO);
        }

//        for (ApRecord record : parentChildrenMap.keySet()) {
//            List<ApRecord> children = parentChildrenMap.get(record);
//
//            List<ApRecordVO> childrenVO = new ArrayList<ApRecordVO>(children.size());
//            ApRecordVO parentVO = parentRecordVOMap.get(record.getId());
//            parentVO.setChilds(childrenVO);
//            for (ApRecord child : children) {
//                Integer partyId = recordIdPartyIdMap.get(child.getId());
//                ApRecordVO apRecordVO = factoryVo.createApRecord(child, partyId, true);
//                childrenVO.add(apRecordVO);
//
//                List<ApRecord> childChildren = apRecordRepository.findByParentRecord(child);
//                apRecordVO.setHasChildren(childChildren.isEmpty() ? false : true);
//            }
//            parentVO.setHasChildren(!childrenVO.isEmpty());
//        }

        return new FilteredResultVO(foundRecordVOList, foundRecordsCount);
    }


    /**
     * Najde seznam rejstříkových hesel, která jsou typu napojeného na dané relationRoleTypeId a mají třídu rejstříku
     * stejnou jako daná osoba.
     *
     * @param search     hledaný řetězec
     * @param from       odkud se mají vracet výsledka
     * @param count      počet vracených výsledků
     * @param roleTypeId id typu vztahu
     * @param partyId    id osoby, ze které je načtena hledaná třída rejstříku
     * @return seznam rejstříkových hesel s počtem všech nalezených
     */
	@Transactional
    @RequestMapping(value = "/findRecordForRelation", method = RequestMethod.GET)
    public FilteredResultVO<ApRecord> findRecordForRelation(@RequestParam(required = false) @Nullable final String search,
                                                            @RequestParam final Integer from,
                                                            @RequestParam final Integer count,
                                                            @RequestParam final Integer roleTypeId,
                                                            @RequestParam final Integer partyId) {

        ParParty party = partyRepository.getOneCheckExist(partyId);

        ParRelationRoleType relationRoleType = relationRoleTypeRepository.getOneCheckExist(roleTypeId);


        Set<Integer> apTypeIds = apTypeRepository.findByRelationRoleType(relationRoleType)
                .stream().map(ApType::getApTypeId).collect(Collectors.toSet());
        apTypeIds = apTypeRepository.findSubtreeIds(apTypeIds);

        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(party.getRecord().getScope().getScopeId());

        final long foundRecordsCount = apRecordRepository.findApRecordByTextAndTypeCount(search, apTypeIds,
                null, scopeIds, true);

        final List<ApRecord> foundRecords = apRecordRepository.findApRecordByTextAndType(search, apTypeIds,
                from, count, null, scopeIds, true);

        List<ApRecordSimple> foundRecordsVO = factoryVo.createApRecordsSimple(foundRecords);
        return new FilteredResultVO(foundRecordsVO, foundRecordsCount);
    }

    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     * @return vytvořený záznam
     */
    @Transactional
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ApRecordVO createRecord(@RequestBody final ApRecordVO record) {
        Assert.isNull(record.getId(), "Při vytváření záznamu nesmí být vyplněno ID (recordId).");

        ApRecord recordDO = factoryDO.createApRecord(record);
        ApRecord newRecordDO = apService.saveRecord(recordDO, false);

        ParParty recordParty = partyService.findParPartyByRecord(newRecordDO);
        return factoryVo.createApRecord(newRecordDO, recordParty == null ? null : recordParty.getPartyId(), false);
    }

	@Transactional
    @RequestMapping(value = "/specificationHasParty/{itemSpecId}", method = RequestMethod.GET)
    public boolean canParty(@PathVariable final Integer itemSpecId) {
        Assert.notNull(itemSpecId, "Identifikátor specifikace musí být vyplněn");

        RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
        Set<Integer> apTypeIds = itemSpecRegisterRepository.findIdsByItemSpecId(spec);
        Set<Integer> apTypeIdTree = apTypeRepository.findSubtreeIds(apTypeIds);

        Integer byItemSpecId = apTypeRepository.findCountPartyTypeNotNullByIds(apTypeIdTree);

        return byItemSpecId > 0;
    }

    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param recordId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
	@Transactional
    @RequestMapping(value = "/{recordId}", method = RequestMethod.GET)
    public ApRecordVO getRecord(@PathVariable final Integer recordId) {
        Assert.notNull(recordId, "Identifikátor rejstříkového hesla musí být vyplněn");

        ApRecord record = apService.getRecord(recordId);

        //seznam nalezeného záznamu spolu s dětmi
        List<ApRecord> records = new LinkedList<>();
        records.add(record);

        //seznam pouze dětí
        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(records);

        Integer partyId = recordIdPartyIdMap.get(recordId);
        ApRecordVO result = factoryVo.createApRecord(record, partyId, true);
        factoryVo.fillApTypeNamesToParents(result);

        result.setVariantRecords(factoryVo.createApVariantRecords(variantRecordRepository.findByApRecordId(recordId)));

        return result;
    }

    /**
     * Aktualizace rejstříkového hesla.
     *
     * @param recordId ID rejstříkového hesla
     * @param record VO rejstříkové heslo
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/{recordId}", method = RequestMethod.PUT)
    public ApRecordVO updateRecord(@PathVariable final Integer recordId, @RequestBody final ApRecordVO record) {
        Assert.notNull(recordId, "Identifikátor rejstříkového hesla musí být vyplněn");
        Assert.notNull(record, "Rejstříkové heslo musí být vyplněno");

        Assert.isTrue(
                recordId.equals(record.getId()),
                "V url požadavku je odkazováno na jiné ID (" + recordId + ") než ve VO (" + record.getId() + ")."
        );
        ApRecord recordTest = apRecordRepository.findOne(record.getId());
        Assert.notNull(recordTest, "Nebyl nalezen záznam pro update s id " + record.getId());

        ApRecord recordDO = factoryDO.createApRecord(record);
        apService.saveRecord(recordDO, false);
        return getRecord(record.getId());
    }

    /**
     * Smazání rejstříkového hesla.
     *
     * @param recordId id rejstříkového hesla
     */
    @Transactional
    @RequestMapping(value = "/{recordId}", method = RequestMethod.DELETE)
    public void deleteRecord(@PathVariable final Integer recordId) {
        Assert.notNull(recordId, "Identifikátor rejstříkového hesla musí být vyplněn");
        ApRecord record = apService.getRecord(recordId);

        apService.deleteRecord(record, true);
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypes", method = RequestMethod.GET)
	@Transactional
    public List<ApTypeVO> getApTypes() {
        List<ApType> allTypes = apTypeRepository.findAllOrderByNameAsc();

        return factoryVo.createApTypesTree(allTypes, false, null);
    }


    /**
     * Vrátí seznam kořenů typů rejstříku (typů hesel) pro typ osoby. Pokud je null, pouze pro typy, které nejsou pro osoby.
     *
     * @return seznam kořenů typů rejstříku (typů hesel)
     */
	@Transactional
    @RequestMapping(value = "/recordTypesForPartyType", method = RequestMethod.GET)
    public List<ApTypeVO> getRecordTypesForPartyType(
            @RequestParam(value = "partyTypeId", required = false) @Nullable final Integer partyTypeId) {

        ParPartyType partyType = null;
        if (partyTypeId != null) {
            partyType = partyTypeRepository.findOne(partyTypeId);
            Assert.notNull(partyType, "Nebyl nalezen typ osoby s id " + partyTypeId);
        }

        List<ApType> allTypes = partyType == null ? apTypeRepository
                .findNullPartyTypeEnableAdding() : apTypeRepository
                                                 .findByPartyTypeEnableAdding(partyType);

        return factoryVo.createApTypesTree(allTypes, true, partyType);
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param variantRecord VO rejstříkové heslo
     * @return vytvořený záznam
     */
    @Transactional
    @RequestMapping(value = "/variantRecord", method = RequestMethod.POST)
    public ApVariantRecordVO createVariantRecord(@RequestBody final ApVariantRecordVO variantRecord) {
        Assert.isNull(variantRecord.getId(), "Při vytváření záznamu nesmí být vyplněno ID (variantRecordId).");

        ApVariantRecord variantRecordDO = factoryDO.createApVariantRecord(variantRecord);

        ApVariantRecord newVariantRecord = apService.saveVariantRecord(variantRecordDO);

        return factoryVo.createApVariantRecord(newVariantRecord);
    }

    /**
     * Aktualizace variantního rejstříkového hesla.
     *
     * @param variantRecordId ID rejstříkové hesla
     * @param variantRecord VO rejstříkové heslo
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/variantRecord/{variantRecordId}", method = RequestMethod.PUT)
    public ApVariantRecordVO updateVariantRecord(@PathVariable final Integer variantRecordId, @RequestBody final ApVariantRecordVO variantRecord) {
        Assert.notNull(variantRecordId, "Identifikátor hesla musí být vyplněn");
        Assert.notNull(variantRecord, "Heslo musí být vyplněno");

        Assert.isTrue(
                variantRecordId.equals(variantRecord.getId()),
                "V url požadavku je odkazováno na jiné ID (" + variantRecordId + ") než ve VO (" + variantRecord.getId() + ")."
        );

        ApVariantRecord variantRecordTest = variantRecordRepository.findOne(variantRecord.getId());
        Assert.notNull(variantRecordTest, "Nebyl nalezen záznam pro update s id " + variantRecord.getId());
        ApVariantRecord variantRecordDO = factoryDO.createApVariantRecord(variantRecord);
        ApVariantRecord updatedVarRec = apService.saveVariantRecord(variantRecordDO);
        apRecordRepository.flush();
        return factoryVo.createApVariantRecord(updatedVarRec);
    }

    /**
     * Smazání variantního rejstříkového hesla.
     *
     * @param variantRecordId id variantního rejstříkového hesla
     */
    @Transactional
    @RequestMapping(value = "/variantRecord/{variantRecordId}", method = RequestMethod.DELETE)
    public void deleteVariantRecord(@PathVariable final Integer variantRecordId) {
        Assert.notNull(variantRecordId, "Identifikátor hesla musí být vyplněn");

        ApVariantRecord variantRecord = apService.getVariantRecord(variantRecordId);
        apService.deleteVariantRecord(variantRecord, variantRecord.getApRecord());
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    @RequestMapping(value = "/scopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getAllScopes(){
        List<ApScope> scopes = scopeRepository.findAllOrderByCode();
        return factoryVo.createScopes(scopes);
    }

    /**
     * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy nastavené v konfiguraci elzy (YAML).
     * @param versionId id verze nebo null
     * @return seznam tříd
     */
    @RequestMapping(value = "/fundScopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getScopeIdsByVersion(@RequestParam(required = false) @Nullable final Integer versionId) {

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze s id " + versionId);
            fund = version.getFund();
        }

        Set<Integer> scopeIdsByFund = apService.getScopeIdsForSearch(fund, null);
        if (CollectionUtils.isEmpty(scopeIdsByFund)) {
            return Collections.emptyList();
        } else {
            List<ApScopeVO> result = factoryVo.createScopes(scopeRepository.findAll(scopeIdsByFund));
            result.sort(Comparator.comparing(ApScopeVO::getCode));
            return result;
        }
    }

    /**
     * Vložení nové třídy.
     *
     * @param scopeVO objekt třídy
     * @return nový objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes", method = RequestMethod.POST)
    public ApScopeVO createScope(@RequestBody final ApScopeVO scopeVO) {
        Assert.notNull(scopeVO, "Scope musí být vyplněn");
        Assert.isNull(scopeVO.getId(), "Identifikátor scope musí být vyplněn");

        ApScope apScope = factoryDO.createScope(scopeVO);

        return factoryVo.createScope(apService.saveScope(apScope));
    }

    /**
     * Aktualizace třídy.
     *
     * @param scopeId id třídy
     * @param scopeVO objekt třídy
     * @return aktualizovaný objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.PUT)
    public ApScopeVO updateScope(@PathVariable final Integer scopeId, @RequestBody final ApScopeVO scopeVO) {
        Assert.notNull(scopeId, "Identifikátor scope musí být vyplněn");
        Assert.notNull(scopeVO, "Scope musí být vyplněn");

        Assert.isTrue(
                scopeId.equals(scopeVO.getId()),
                "V url požadavku je odkazováno na jiné ID (" + scopeId + ") než ve VO (" + scopeVO.getId() + ")."
        );

        ApScope apScope = factoryDO.createScope(scopeVO);
        return factoryVo.createScope(apService.saveScope(apScope));
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param scopeId id třídy.
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.DELETE)
    public void deleteScope(@PathVariable final Integer scopeId) {
        ApScope scope = scopeRepository.findOne(scopeId);
        apService.deleteScope(scope);
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @RequestMapping(value = "/externalSystems", method = RequestMethod.GET)
	@Transactional
    public List<ApExternalSystemSimpleVO> findAllExternalSystems() {
		List<ApExternalSystem> extSystems = externalSystemService.findAllApSystem();
		return factoryVo.createSimpleEntity(extSystems, ApExternalSystemSimpleVO.class);
    }

    /**
     * Aktualizace rejstříku z externího systému.
     * @param recordId id rejstříku
     * @param recordImportVO data rejstříku
     */
    @RequestMapping(value = "/interpi/import/{recordId}", method = RequestMethod.PUT)
    @Transactional
    public ApRecordVO updateRecord(@PathVariable final Integer recordId, @RequestBody final RecordImportVO recordImportVO) {
        Assert.notNull(recordId, "Identifikátor rejstříkového hesla musí být vyplněn");
        Assert.notNull(recordImportVO, "Struktura importu hesla musí být vyplněna");
        Assert.notNull(recordImportVO.getInterpiRecordId(), "Identifikátor interpi musí být vyplněn");
        Assert.notNull(recordImportVO.getScopeId(), "Identifikátor scope musí být vyplněn");
        Assert.notNull(recordImportVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        interpiService.importRecord(recordId, recordImportVO.getInterpiRecordId(), recordImportVO.getScopeId(),
                recordImportVO.getSystemId(), recordImportVO.getOriginator(), recordImportVO.getMappings());

        return getRecord(recordId);
    }

    /**
     * Založení rejstříku z externího systému.
     * @param recordImportVO data rejstříku
     */
    @Transactional
    @RequestMapping(value = "/interpi/import", method = RequestMethod.POST)
    public ApRecordVO importRecord(@RequestBody final RecordImportVO recordImportVO) {
        Assert.notNull(recordImportVO, "Struktura importu hesla musí být vyplněna");
        Assert.notNull(recordImportVO.getInterpiRecordId(), "Identifikátor interpi musí být vyplněn");
        Assert.notNull(recordImportVO.getScopeId(), "Identifikátor scope musí být vyplněn");
        Assert.notNull(recordImportVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        ApRecord apRecord = interpiService.importRecord(null, recordImportVO.getInterpiRecordId(), recordImportVO.getScopeId(),
                recordImportVO.getSystemId(), recordImportVO.getOriginator(), recordImportVO.getMappings());

        return getRecord(apRecord.getRecordId());
    }

    /**
     * Vyhledá rejstříky v externím systému.
     *
     * @param interpiSearchVO vyhledávací kritéria
     *
     * @return rejstřík z externího systému
     */
    @Transactional
    @RequestMapping(value = "/interpi", method = RequestMethod.POST)
    public List<ExternalRecordVO> findInterpiRecords(@RequestBody final InterpiSearchVO interpiSearchVO) {
        Assert.notNull(interpiSearchVO, "Struktura pro vyhledání musí být vyplněna");
        Assert.notNull(interpiSearchVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        long start = System.currentTimeMillis();
        List<ExternalRecordVO> records = interpiService.findRecords(interpiSearchVO.isParty(), interpiSearchVO.getConditions(),
                interpiSearchVO.getCount(), interpiSearchVO.getSystemId());
        long end = System.currentTimeMillis();
        logger.debug("Nalezení " + records.size() + " záznamů, trvalo " + (end - start) + " ms.");

        return records;
    }

    /**
     * Načte vztahy daného záznamu.
     *
     * @param interpiRecordId id rejstříku v INTERPI
     * @param relationSearchVO vyhledávávací kriteria
     *
     * @return vztahy a jejich mapování
     */
	@Transactional
    @RequestMapping(value = "/interpi/{interpiRecordId}/relations", method = RequestMethod.POST)
    public InterpiMappingVO findInterpiRecordRelations(@PathVariable final String interpiRecordId, @RequestBody final RelationSearchVO relationSearchVO) {
        Assert.notNull(interpiRecordId, "Identifikátor systému interpi musí být vyplněn");
        Assert.notNull(relationSearchVO, "Struktura importu hesla musí být vyplněna");
        Assert.notNull(relationSearchVO.getScopeId(), "Identifikátor scope musí být vyplněn");
        Assert.notNull(relationSearchVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        return interpiService.findInterpiRecordRelations(interpiRecordId, relationSearchVO.getSystemId(), relationSearchVO.getScopeId());
    }

    /**
     * Najde použití rejstříku.
     *
     * @param recordId identifikátor rejstříku
     *
     * @return použití rejstříku
     */
    @RequestMapping(value = "/{recordId}/usage", method = RequestMethod.GET)
    @Transactional
    public RecordUsageVO findUsage(@PathVariable final Integer recordId) {
    	ApRecord apRecord = apRecordRepository.getOneCheckExist(recordId);
    	ParParty parParty = partyService.findParPartyByRecord(apRecord);
    	return apService.findRecordUsage(apRecord, parParty);
    }

    /**
     * Nahrazení rejstříku
     *
     * @param recordId ID nahrazovaného rejstříku
     * @param replacedId ID rejstříku kterým budeme nahrazovat
     */
    @Transactional
    @RequestMapping(value = "/{recordId}/replace", method = RequestMethod.POST)
    public void replace(@PathVariable final Integer recordId, @RequestBody final Integer replacedId) {
        final ApRecord replaced = apService.getRecord(recordId);
        final ApRecord replacement = apService.getRecord(replacedId);

        final ParParty replacedParty = partyService.findParPartyByRecord(replaced);

        if (replacedParty != null) {
            final ParParty replacementParty = partyService.findParPartyByRecord(replacement);
            if (replacementParty == null) {
                throw new BusinessException("Osobu lze nahradit pouze osobou.", BaseCode.INVALID_STATE);
            }
            partyService.replace(replacedParty, replacementParty);
        } else {
            apService.replace(replaced, replacement);
        }
    }

    /**
     * Zplatnění rejstříkového hesla
     * @param recordId rejstřík id
     */
    @Transactional
    @RequestMapping(value = "/{recordId}/valid", method = RequestMethod.POST)
    public void valid(@PathVariable final Integer recordId) {
        final ApRecord record = apService.getRecord(recordId);
        record.setInvalid(false);
        apService.saveRecord(record, false);
    }
}
