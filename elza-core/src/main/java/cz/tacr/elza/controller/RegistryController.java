package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import cz.tacr.elza.controller.vo.InterpiMappingVO;
import cz.tacr.elza.controller.vo.InterpiSearchVO;
import cz.tacr.elza.controller.vo.RecordImportVO;
import cz.tacr.elza.controller.vo.RegCoordinatesVO;
import cz.tacr.elza.controller.vo.RegExternalSystemVO;
import cz.tacr.elza.controller.vo.RegRecordSimple;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RelationSearchVO;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.UserService;


/**
 * REST Kontrolér pro registry.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@RestController
@RequestMapping(value = "/api/registry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class RegistryController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private RegCoordinatesRepository regCoordinatesRepository;

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
    private UserService userService;

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
     * @param registerTypeId   IDčka typu záznamu, může být null
     * @param parentRecordId    id rodiče, pokud je null načtou se všechny záznamy, jinak potomci daného rejstříku
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @param itemSpecId   id specifikace
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public FilteredResultVO<RegRecord> findRecord(@RequestParam(required = false) @Nullable final String search,
                                       @RequestParam final Integer from,
                                       @RequestParam final Integer count,
                                       @RequestParam(required = false) @Nullable final Integer registerTypeId,
                                       @RequestParam(required = false) @Nullable final Integer parentRecordId,
                                       @RequestParam(required = false) @Nullable final Integer versionId,
                                       @RequestParam(required = false) @Nullable final Integer itemSpecId) {

        Set<Integer> registerTypeIdTree = Collections.emptySet();

        if (itemSpecId != null && registerTypeId != null) {
            throw new SystemException("Nelza použít specifikaci a typ rejstříku zároveň.", BaseCode.SYSTEM_ERROR);
        } else if (itemSpecId != null || registerTypeId != null) {
            Set<Integer> registerTypeIds = new HashSet<>();
            if (itemSpecId != null) {
                RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
                registerTypeIds.addAll(itemSpecRegisterRepository.findIdsByItemSpecId(spec));
            } else {
                registerTypeIds.add(registerTypeId);
            }
            registerTypeIdTree = registerTypeRepository.findSubtreeIds(registerTypeIds);
        }

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
            fund = version.getFund();
        }

        if(parentRecordId != null) {
            regRecordRepository.getOneCheckExist(parentRecordId);
        }

        final long foundRecordsCount = registryService.findRegRecordByTextAndTypeCount(search, registerTypeIdTree, parentRecordId, fund);

        List<RegRecord> foundRecords = registryService
                .findRegRecordByTextAndType(search, registerTypeIdTree, from, count, parentRecordId, fund);


        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(foundRecords);

        List<RegRecordVO> foundRecordVOList = factoryVo.createRegRecords(foundRecords, recordIdPartyIdMap, true);

        Map<Integer, RegRecordVO> parentRecordVOMap = new HashMap<>();
        Map<RegRecord, List<RegRecord>> parentChildrenMap = registryService.findChildren(foundRecords);

        for (RegRecordVO regRecordVO : foundRecordVOList) {
            parentRecordVOMap.put(regRecordVO.getId(), regRecordVO);
            factoryVo.fillRegisterTypeNamesToParents(regRecordVO);
        }



        // děti
        foundRecords.forEach(record -> {
            List<RegRecord> children = parentChildrenMap.get(record);
            parentRecordVOMap.get(record.getRecordId()).setHasChildren(children != null && !children.isEmpty());
        });


//        for (RegRecord record : parentChildrenMap.keySet()) {
//            List<RegRecord> children = parentChildrenMap.get(record);
//
//            List<RegRecordVO> childrenVO = new ArrayList<RegRecordVO>(children.size());
//            RegRecordVO parentVO = parentRecordVOMap.get(record.getId());
//            parentVO.setChilds(childrenVO);
//            for (RegRecord child : children) {
//                Integer partyId = recordIdPartyIdMap.get(child.getId());
//                RegRecordVO regRecordVO = factoryVo.createRegRecord(child, partyId, true);
//                childrenVO.add(regRecordVO);
//
//                List<RegRecord> childChildren = regRecordRepository.findByParentRecord(child);
//                regRecordVO.setHasChildren(childChildren.isEmpty() ? false : true);
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
    @RequestMapping(value = "/findRecordForRelation", method = RequestMethod.GET)
    public FilteredResultVO<RegRecord> findRecordForRelation(@RequestParam(required = false) @Nullable final String search,
                                                    @RequestParam final Integer from,
                                                    @RequestParam final Integer count,
                                                    @RequestParam final Integer roleTypeId,
                                                    @RequestParam final Integer partyId) {

        ParParty party = partyRepository.getOneCheckExist(partyId);

        ParRelationRoleType relationRoleType = relationRoleTypeRepository.getOneCheckExist(roleTypeId);


        Set<Integer> registerTypeIds = registerTypeRepository.findByRelationRoleType(relationRoleType)
                .stream().map(RegRegisterType::getRegisterTypeId).collect(Collectors.toSet());
        registerTypeIds = registerTypeRepository.findSubtreeIds(registerTypeIds);

        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(party.getRecord().getScope().getScopeId());

        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_RD_ALL);

        final long foundRecordsCount = regRecordRepository
                .findRegRecordByTextAndTypeCount(search, registerTypeIds, null, scopeIds, readAllScopes, user);

        final List<RegRecord> foundRecords = regRecordRepository
                .findRegRecordByTextAndType(search, registerTypeIds, from, count, null, scopeIds, readAllScopes, user);

        List<RegRecordSimple> foundRecordsVO = factoryVo.createRegRecordsSimple(foundRecords);
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
    public RegRecordVO createRecord(@RequestBody final RegRecordVO record) {
        Assert.isNull(record.getId(), "Při vytváření záznamu nesmí být vyplněno ID (recordId).");

        RegRecord recordDO = factoryDO.createRegRecord(record);
        RegRecord newRecordDO = registryService.saveRecord(recordDO, false);

        ParParty recordParty = partyService.findParPartyByRecord(newRecordDO);
        return factoryVo.createRegRecord(newRecordDO, recordParty == null ? null : recordParty.getPartyId(), false);
    }

    @RequestMapping(value = "/specificationHasParty/{itemSpecId}", method = RequestMethod.GET)
    public boolean canParty(@PathVariable final Integer itemSpecId) {
        Assert.notNull(itemSpecId);

        RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
        Set<Integer> registerTypeIds = itemSpecRegisterRepository.findIdsByItemSpecId(spec);
        Set<Integer> registerTypeIdTree = registerTypeRepository.findSubtreeIds(registerTypeIds);

        Integer byItemSpecId = registerTypeRepository.findCountPartyTypeNotNullByIds(registerTypeIdTree);

        return byItemSpecId > 0;
    }

    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param recordId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
    @RequestMapping(value = "/{recordId}", method = RequestMethod.GET)
    public RegRecordVO getRecord(@PathVariable final Integer recordId) {
        Assert.notNull(recordId);

        RegRecord record = registryService.getRecord(recordId);

        //seznam nalezeného záznamu spolu s dětmi
        List<RegRecord> records = new LinkedList<>();
        records.add(record);

        //seznam pouze dětí
        List<RegRecord> childs = regRecordRepository.findByParentRecord(record);
        records.addAll(childs);


        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(records);

        Integer partyId = recordIdPartyIdMap.get(recordId);
        RegRecordVO result = factoryVo.createRegRecord(record, partyId, true);
        factoryVo.fillRegisterTypeNamesToParents(result);
        result.setChilds(factoryVo.createRegRecords(childs, recordIdPartyIdMap, false));

        result.setVariantRecords(factoryVo.createRegVariantRecords(variantRecordRepository.findByRegRecordId(recordId)));

        result.setCoordinates(factoryVo.createRegCoordinates(regCoordinatesRepository.findByRegRecordId(recordId)));

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
    public RegRecordVO updateRecord(@PathVariable final Integer recordId, @RequestBody final RegRecordVO record) {
        Assert.notNull(recordId);
        Assert.notNull(record);

        Assert.isTrue(
                recordId.equals(record.getId()),
                "V url požadavku je odkazováno na jiné ID (" + recordId + ") než ve VO (" + record.getId() + ")."
        );
        RegRecord recordTest = regRecordRepository.findOne(record.getId());
        Assert.notNull(recordTest, "Nebyl nalezen záznam pro update s id " + record.getId());

        RegRecord recordDO = factoryDO.createRegRecord(record);
        registryService.saveRecord(recordDO, false);
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
        Assert.notNull(recordId);
        RegRecord record = regRecordRepository.getOneCheckExist(recordId);

        registryService.deleteRecord(record, true);
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypes", method = RequestMethod.GET)
    public List<RegRegisterTypeVO> getRecordTypes() {
        List<RegRegisterType> allTypes = registerTypeRepository.findAllOrderByNameAsc();

        return factoryVo.createRegisterTypesTree(allTypes, false, null);
    }


    /**
     * Vrátí seznam kořenů typů rejstříku (typů hesel) pro typ osoby. Pokud je null, pouze pro typy, které nejsou pro osoby.
     *
     * @return seznam kořenů typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypesForPartyType", method = RequestMethod.GET)
    public List<RegRegisterTypeVO> getRecordTypesForPartyType(
            @RequestParam(value = "partyTypeId", required = false) @Nullable final Integer partyTypeId) {

        ParPartyType partyType = null;
        if (partyTypeId != null) {
            partyType = partyTypeRepository.findOne(partyTypeId);
            Assert.notNull(partyType, "Nebyl nalezen typ osoby s id " + partyTypeId);
        }

        List<RegRegisterType> allTypes = partyType == null ? registerTypeRepository
                .findNullPartyTypeEnableAdding() : registerTypeRepository
                                                 .findByPartyTypeEnableAdding(partyType);

        return factoryVo.createRegisterTypesTree(allTypes, true, partyType);
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param variantRecord VO rejstříkové heslo
     * @return vytvořený záznam
     */
    @Transactional
    @RequestMapping(value = "/variantRecord", method = RequestMethod.POST)
    public RegVariantRecordVO createVariantRecord(@RequestBody final RegVariantRecordVO variantRecord) {
        Assert.isNull(variantRecord.getId(), "Při vytváření záznamu nesmí být vyplněno ID (variantRecordId).");

        RegVariantRecord variantRecordDO = factoryDO.createRegVariantRecord(variantRecord);

        RegVariantRecord newVariantRecord = registryService.saveVariantRecord(variantRecordDO);

        return factoryVo.createRegVariantRecord(newVariantRecord);
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
    public RegVariantRecordVO updateVariantRecord(@PathVariable final Integer variantRecordId, @RequestBody final RegVariantRecordVO variantRecord) {
        Assert.notNull(variantRecordId);
        Assert.notNull(variantRecord);

        Assert.isTrue(
                variantRecordId.equals(variantRecord.getId()),
                "V url požadavku je odkazováno na jiné ID (" + variantRecordId + ") než ve VO (" + variantRecord.getId() + ")."
        );

        RegVariantRecord variantRecordTest = variantRecordRepository.findOne(variantRecord.getId());
        Assert.notNull(variantRecordTest, "Nebyl nalezen záznam pro update s id " + variantRecord.getId());
        RegVariantRecord variantRecordDO = factoryDO.createRegVariantRecord(variantRecord);
        RegVariantRecord updatedVarRec = registryService.saveVariantRecord(variantRecordDO);
        regRecordRepository.flush();
        return factoryVo.createRegVariantRecord(updatedVarRec);
    }

    /**
     * Smazání variantního rejstříkového hesla.
     *
     * @param variantRecordId id variantního rejstříkového hesla
     */
    @Transactional
    @RequestMapping(value = "/variantRecord/{variantRecordId}", method = RequestMethod.DELETE)
    public void deleteVariantRecord(@PathVariable final Integer variantRecordId) {
        Assert.notNull(variantRecordId);

        RegVariantRecord variantRecord = registryService.getVariantRecord(variantRecordId);
        registryService.deleteVariantRecord(variantRecord, variantRecord.getRegRecord());
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    @RequestMapping(value = "/scopes", method = RequestMethod.GET)
    public List<RegScopeVO> getAllScopes(){
        List<RegScope> scopes = scopeRepository.findAllOrderByCode();
        return factoryVo.createScopes(scopes);
    }

    /**
     * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy nastavené v konfiguraci elzy (YAML).
     * @param versionId id verze nebo null
     * @return seznam tříd
     */
    @RequestMapping(value = "/fundScopes", method = RequestMethod.GET)
    public List<RegScopeVO> getScopeIdsByVersion(@RequestParam(required = false) @Nullable final Integer versionId) {

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze s id " + versionId);
            fund = version.getFund();
        }

        Set<Integer> scopeIdsByFund = registryService.getScopeIdsByFund(fund);
        if (CollectionUtils.isEmpty(scopeIdsByFund)) {
            return Collections.emptyList();
        } else {
            List<RegScopeVO> result = factoryVo.createScopes(scopeRepository.findAll(scopeIdsByFund));
            result.sort(Comparator.comparing(RegScopeVO::getCode));
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
    public RegScopeVO createScope(@RequestBody final RegScopeVO scopeVO) {
        Assert.notNull(scopeVO);
        Assert.isNull(scopeVO.getId());

        RegScope regScope = factoryDO.createScope(scopeVO);

        return factoryVo.createScope(registryService.saveScope(regScope));
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
    public RegScopeVO updateScope(@PathVariable final Integer scopeId, @RequestBody final RegScopeVO scopeVO) {
        Assert.notNull(scopeId);
        Assert.notNull(scopeVO);

        Assert.isTrue(
                scopeId.equals(scopeVO.getId()),
                "V url požadavku je odkazováno na jiné ID (" + scopeId + ") než ve VO (" + scopeVO.getId() + ")."
        );

        RegScope regScope = factoryDO.createScope(scopeVO);
        return factoryVo.createScope(registryService.saveScope(regScope));
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param scopeId id třídy.
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.DELETE)
    public void deleteScope(@PathVariable final Integer scopeId) {
        RegScope scope = scopeRepository.findOne(scopeId);
        registryService.deleteScope(scope);
    }

    /**
     * Vrací výchozí třídy rejstříků z databáze.
     */
    @RequestMapping(value = "/defaultScopes", method = RequestMethod.GET)
    public List<RegScopeVO> getDefaultScopes() {
        List<RegScope> scopes = registryService.findDefaultScopes();
        return factoryVo.createScopes(scopes);
    }

    /**
     * Vytvoří nové souřadnice k rejsříkovému heslu
     */
    @Transactional
    @RequestMapping(value = "/regCoordinates", method = RequestMethod.POST)
    public RegCoordinatesVO createRegCoordinates(@RequestBody final RegCoordinatesVO coordinatesVO) {
        Assert.isNull(coordinatesVO.getId(),
                "Při vytváření záznamu nesmí být vyplněno ID (coordinatesId).");
        Assert.isTrue(StringUtils.isNotEmpty(coordinatesVO.getValue()), "Nutno vyplnit hodnotu!");
        RegCoordinates coordinates = factoryDO.createRegCoordinates(coordinatesVO);
        coordinates = registryService.saveRegCoordinates(coordinates);
        return factoryVo.createRegCoordinates(coordinates);
    }

    /**
     * Aktualizace souřadnic rejstříkového hesla.
     *
     * @param coordinatesVO VO souřadnice hesla
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/regCoordinates/{coordinatesId}", method = RequestMethod.PUT)
    public RegCoordinatesVO updateRegCoordinates(@PathVariable final Integer coordinatesId, @RequestBody final RegCoordinatesVO coordinatesVO) {
        Assert.notNull(coordinatesId);
        Assert.notNull(coordinatesVO);

        Assert.isTrue(
                coordinatesId.equals(coordinatesVO.getId()),
                "V url požadavku je odkazováno na jiné ID (" + coordinatesId + ") než ve VO (" + coordinatesVO.getId() + ")."
        );

        RegCoordinates coordinates = regCoordinatesRepository.findOne(coordinatesVO.getId());
        Assert.notNull(coordinates, "Nebyl nalezen záznam pro update s id " + coordinatesVO.getId());
        Assert.isTrue(StringUtils.isNotEmpty(coordinatesVO.getValue()), "Nutno vyplnit hodnotu!");
        RegCoordinates coordinatesDO = factoryDO.createRegCoordinates(coordinatesVO);

        if (!"Point".equals(coordinates.getValue().getGeometryType())) {
            coordinatesDO.setValue(coordinates.getValue());
        }

        coordinates = registryService.saveRegCoordinates(coordinatesDO);
        regRecordRepository.flush();
        return factoryVo.createRegCoordinates(coordinates);
    }

    /**
     * Smazání souřadnic rejstříkového hesla.
     *
     * @param coordinatesId id souřadnic rejstříkového hesla
     */
    @Transactional
    @RequestMapping(value = "/regCoordinates/{coordinatesId}", method = RequestMethod.DELETE)
    public void deleteRegCoordinates(@PathVariable final Integer coordinatesId) {
        Assert.notNull(coordinatesId);
        RegCoordinates regCoordinate = registryService.getRegCoordinate(coordinatesId);

        registryService.deleteRegCoordinate(regCoordinate, regCoordinate.getRegRecord());
    }


    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @RequestMapping(value = "/externalSystems", method = RequestMethod.GET)
    public List<RegExternalSystemVO> findAllExternalSystems() {
        return factoryVo.createSimpleEntity(externalSystemService.findAllRegSystem(), RegExternalSystemVO.class);
    }

    /**
     * Aktualizace rejstříku z externího systému.
     * @param recordId id rejstříku
     * @param recordImportVO data rejstříku
     */
    @RequestMapping(value = "/interpi/import/{recordId}", method = RequestMethod.PUT)
    @Transactional
    public RegRecordVO updateRecord(@PathVariable final Integer recordId, @RequestBody final RecordImportVO recordImportVO) {
        Assert.notNull(recordId);
        Assert.notNull(recordImportVO);
        Assert.notNull(recordImportVO.getInterpiRecordId());
        Assert.notNull(recordImportVO.getScopeId());
        Assert.notNull(recordImportVO.getSystemId());

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
    public RegRecordVO importRecord(@RequestBody final RecordImportVO recordImportVO) {
        Assert.notNull(recordImportVO);
        Assert.notNull(recordImportVO.getInterpiRecordId());
        Assert.notNull(recordImportVO.getScopeId());
        Assert.notNull(recordImportVO.getSystemId());

        RegRecord regRecord = interpiService.importRecord(null, recordImportVO.getInterpiRecordId(), recordImportVO.getScopeId(),
                recordImportVO.getSystemId(), recordImportVO.getOriginator(), recordImportVO.getMappings());

        return getRecord(regRecord.getRecordId());
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
        Assert.notNull(interpiSearchVO);
        Assert.notNull(interpiSearchVO.getSystemId());

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
    @RequestMapping(value = "/interpi/{interpiRecordId}/relations", method = RequestMethod.POST)
    public InterpiMappingVO findInterpiRecordRelations(@PathVariable final String interpiRecordId, @RequestBody final RelationSearchVO relationSearchVO) {
        Assert.notNull(interpiRecordId);
        Assert.notNull(relationSearchVO);
        Assert.notNull(relationSearchVO.getScopeId());
        Assert.notNull(relationSearchVO.getSystemId());

        return interpiService.findInterpiRecordRelations(interpiRecordId, relationSearchVO.getSystemId(), relationSearchVO.getScopeId());
    }
}
