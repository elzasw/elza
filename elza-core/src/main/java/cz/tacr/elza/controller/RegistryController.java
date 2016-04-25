package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.HashMap;
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
import cz.tacr.elza.repository.*;
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
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;


/**
 * REST Kontrolér pro registry.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@RestController
@RequestMapping("/api/registryManagerV2")
public class RegistryController {

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegistryService registryService;

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
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET)
    public RegRecordWithCount findRecord(@RequestParam(required = false) @Nullable final String search,
                                         @RequestParam final Integer from, @RequestParam final Integer count,
                                         @RequestParam(value = "registerTypeId", required = false) @Nullable final Integer registerTypeId,
                                         @RequestParam(required = false) @Nullable final Integer parentRecordId,
                                         @RequestParam(required = false) @Nullable final Integer versionId) {

        Set<Integer> registerTypeIdTree = Collections.EMPTY_SET;
        if (registerTypeId != null) {
            Set<Integer> registerTypeIds = new HashSet<>();
            registerTypeIds.add(registerTypeId);

            registerTypeIdTree = registerTypeRepository.findSubtreeIds(registerTypeIds);
        }

        ArrFund fund;
        if(versionId == null){
            fund = null;
        }else{
            ArrFundVersion version = fundVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze archivní pomůcky s id "+versionId);
            fund = version.getFund();
        }



        RegRecord parentRecord = null;
        if(parentRecordId != null) {
            parentRecord = regRecordRepository.findOne(parentRecordId);
            Assert.notNull(parentRecord, "Nebylo nalezeno rejstříkové heslo s id " + parentRecordId);
        }

        final long foundRecordsCount = registryService.findRegRecordByTextAndTypeCount(search, registerTypeIdTree,
                parentRecordId, fund);

        List<RegRecord> foundRecords = registryService
                .findRegRecordByTextAndType(search, registerTypeIdTree, from, count, parentRecordId, fund);


        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(foundRecords);

        List<RegRecordVO> foundRecordVOList = factoryVo
                .createRegRecords(foundRecords, recordIdPartyIdMap, true, parentRecord);

        Map<Integer, RegRecordVO> parentRecordVOMap = new HashMap<>();
        Map<RegRecord, List<RegRecord>> parentChildrenMap = registryService.findChildren(foundRecords);

        for (RegRecordVO regRecordVO : foundRecordVOList) {
            parentRecordVOMap.put(regRecordVO.getRecordId(), regRecordVO);
            factoryVo.fillRegisterTypeNamesToParents(regRecordVO);
        }



        // děti
        foundRecords.forEach(record -> {
            List<RegRecord> children = parentChildrenMap.get(record);
            parentRecordVOMap.get(record.getRecordId()).setHasChildren(children == null ? false : !children.isEmpty());
        });


//        for (RegRecord record : parentChildrenMap.keySet()) {
//            List<RegRecord> children = parentChildrenMap.get(record);
//
//            List<RegRecordVO> childrenVO = new ArrayList<RegRecordVO>(children.size());
//            RegRecordVO parentVO = parentRecordVOMap.get(record.getRecordId());
//            parentVO.setChilds(childrenVO);
//            for (RegRecord child : children) {
//                Integer partyId = recordIdPartyIdMap.get(child.getRecordId());
//                RegRecordVO regRecordVO = factoryVo.createRegRecord(child, partyId, true);
//                childrenVO.add(regRecordVO);
//
//                List<RegRecord> childChildren = regRecordRepository.findByParentRecord(child);
//                regRecordVO.setHasChildren(childChildren.isEmpty() ? false : true);
//            }
//            parentVO.setHasChildren(!childrenVO.isEmpty());
//        }

        return new RegRecordWithCount(foundRecordVOList, foundRecordsCount);
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
    public RegRecordWithCount findRecordForRelation(@RequestParam(required = false) @Nullable final String search,
                                                    @RequestParam final Integer from, @RequestParam final Integer count,
                                                    @RequestParam(required = true) @Nullable final Integer roleTypeId,
                                                    @RequestParam(required = true) @Nullable final Integer partyId) {

        ParParty party = partyRepository.findOne(partyId);
        Assert.notNull(party, "Nebyla nalezena osoba s id " + partyId);

        ParRelationRoleType relationRoleType = relationRoleTypeRepository.findOne(roleTypeId);
        Assert.notNull(roleTypeId, "Nebyl nalezen typ vztahu s id " + roleTypeId);

        Set<Integer> registerTypeIds = registerTypeRepository.findByRelationRoleType(relationRoleType)
                .stream().map(t -> t.getRegisterTypeId()).collect(Collectors.toSet());
        registerTypeIds = registerTypeRepository.findSubtreeIds(registerTypeIds);

        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(party.getRecord().getScope().getScopeId());


        final long foundRecordsCount = regRecordRepository
                .findRegRecordByTextAndTypeCount(search, registerTypeIds, null, scopeIds);

        final List<RegRecord> foundRecords = regRecordRepository
                .findRegRecordByTextAndType(search, registerTypeIds, from, count, null, scopeIds);

        List<RegRecordSimple> foundRecordsVO = factoryVo.createRegRecordsSimple(foundRecords);
        return new RegRecordWithCount(foundRecordsVO, foundRecordsCount);
    }


    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param recordId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
    @RequestMapping(value = "/getRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"recordId"})
    public RegRecordVO getRecord(@RequestParam(value = "recordId") final Integer recordId) {
        Assert.notNull(recordId);

        RegRecord record = regRecordRepository.getOne(recordId);

        //seznam nalezeného záznamu spolu s dětmi
        List<RegRecord> records = new LinkedList<>();
        records.add(record);

        //seznam pouze dětí
        List<RegRecord> childs = regRecordRepository.findByParentRecord(record);
        records.addAll(childs);


        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(records);

        Integer partyId = recordIdPartyIdMap.get(recordId);
        RegRecordVO result = factoryVo.createRegRecord(record, partyId, true, null);
        factoryVo.fillRegisterTypeNamesToParents(result);
        result.setChilds(factoryVo.createRegRecords(childs, recordIdPartyIdMap, false, null));

        result.setVariantRecords(factoryVo.createRegVariantRecords(variantRecordRepository.findByRegRecordId(recordId)));

        result.setCoordinates(factoryVo.createRegCoordinates(regCoordinatesRepository.findByRegRecordId(recordId)));

        return result;
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRegisterTypeVO> getRecordTypes(){
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
     * Vytvoření rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     * @return vytvořený záznam
     */
    @Transactional
    @RequestMapping(value = "/createRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegRecordVO createRecord(@RequestBody final RegRecordVO record) {
        Assert.isNull(record.getRecordId(), "Při vytváření záznamu nesmí být vyplněno ID (recordId).");

        RegRecord recordDO = factoryDO.createRegRecord(record);
        RegRecord newRecordDO = registryService.saveRecord(recordDO, false);

        ParParty recordParty = partyService.findParPartyByRecord(newRecordDO);
        return factoryVo.createRegRecord(newRecordDO, recordParty == null ? null : recordParty.getPartyId(), false,
                null);
    }

    /**
     * Aktualizace rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/updateRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegRecordVO updateRecord(@RequestBody final RegRecordVO record) {
        Assert.notNull(record.getRecordId(), "Očekáváno ID (recordId) pro update.");
        RegRecord recordTest = regRecordRepository.findOne(record.getRecordId());
        Assert.notNull(recordTest, "Nebyl nalezen záznam pro update s id " + record.getRecordId());

        RegRecord recordDO = factoryDO.createRegRecord(record);
        RegRecord newRecordDO = registryService.saveRecord(recordDO, false);
        return getRecord(record.getRecordId());
    }



    /**
     * Smazání rejstříkového hesla.
     *
     * @param recordId id rejstříkového hesla
     */
    @Transactional
    @RequestMapping(value = "/deleteRecord", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"recordId"})
    public void deleteRecord(@RequestParam(value = "recordId") final Integer recordId) {
        Assert.notNull(recordId);
        RegRecord record = regRecordRepository.findOne(recordId);
        if (record == null) {
            return;
        }

        registryService.deleteRecord(record, true);
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param variantRecord VO rejstříkové heslo
     * @return vytvořený záznam
     */
    @Transactional
    @RequestMapping(value = "/createVariantRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegVariantRecordVO createVariantRecord(@RequestBody final RegVariantRecordVO variantRecord) {
        Assert.isNull(variantRecord.getVariantRecordId(),
                "Při vytváření záznamu nesmí být vyplněno ID (variantRecordId).");

        RegVariantRecord variantRecordDO = factoryDO.createRegVariantRecord(variantRecord);

        RegVariantRecord newVariantRecord = registryService.saveVariantRecord(variantRecordDO);
        return factoryVo.createRegVariantRecord(newVariantRecord);
    }

    /**
     * Aktualizace variantního rejstříkového hesla.
     *
     * @param variantRecord VO rejstříkové heslo
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/updateVariantRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegVariantRecordVO updateVariantRecord(@RequestBody final RegVariantRecordVO variantRecord) {
        Assert.notNull(variantRecord.getVariantRecordId(), "Očekáváno ID pro update.");
        RegVariantRecord variantRecordTest = variantRecordRepository.findOne(variantRecord.getVariantRecordId());
        Assert.notNull(variantRecordTest, "Nebyl nalezen záznam pro update s id " + variantRecord.getVariantRecordId());
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
    @RequestMapping(value = "/deleteVariantRecord", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"variantRecordId"})
    public void deleteVariantRecord(@RequestParam(value = "variantRecordId") final Integer variantRecordId) {
        Assert.notNull(variantRecordId);
        RegVariantRecord variantRecord = variantRecordRepository.findOne(variantRecordId);
        if (variantRecord == null) {
            return;
        }

        variantRecordRepository.delete(variantRecordId);
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
        if (scopeIdsByFund.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List<RegScopeVO> result = factoryVo.createScopes(scopeRepository.findAll(scopeIdsByFund));
            result.sort((a, b) -> a.getCode().compareTo(b.getCode()));
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
    public RegScopeVO updateScope(
            @PathVariable(value = "scopeId") final Integer scopeId,
            @RequestBody final RegScopeVO scopeVO) {
        Assert.notNull(scopeVO);
        scopeVO.setId(scopeId);

        RegScope regScope = factoryDO.createScope(scopeVO);
        return factoryVo.createScope(registryService.saveScope(regScope));
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param scopeId id třídy.
     */
    @Transactional
    @RequestMapping(value = "/scopes", method = RequestMethod.DELETE)
    public void deleteScope(@RequestParam final Integer scopeId) {
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
    @RequestMapping(value = "/createRegCoordinates", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegCoordinatesVO createRegCoordinates(@RequestBody final RegCoordinatesVO coordinatesVO) {
        Assert.isNull(coordinatesVO.getCoordinatesId(),
                "Při vytváření záznamu nesmí být vyplněno ID (coordinatesId).");
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
    @RequestMapping(value = "/updateRegCoordinates", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegCoordinatesVO updateRegCoordinates(@RequestBody final RegCoordinatesVO coordinatesVO) {
        Assert.notNull(coordinatesVO.getCoordinatesId(), "Očekáváno ID pro update.");
        RegCoordinates coordinates = regCoordinatesRepository.findOne(coordinatesVO.getCoordinatesId());
        Assert.notNull(coordinates, "Nebyl nalezen záznam pro update s id " + coordinatesVO.getCoordinatesId());
        RegCoordinates coordinatesDO = factoryDO.createRegCoordinates(coordinatesVO);
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
    @RequestMapping(value = "/deleteRegCoordinates", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"coordinatesId"})
    public void deleteRegCoordinates(@RequestParam(value = "coordinatesId") final Integer coordinatesId) {
        Assert.notNull(coordinatesId);
        RegCoordinates variantRecord = regCoordinatesRepository.findOne(coordinatesId);
        if (variantRecord == null) {
            return;
        }

        regCoordinatesRepository.delete(coordinatesId);
    }
}
