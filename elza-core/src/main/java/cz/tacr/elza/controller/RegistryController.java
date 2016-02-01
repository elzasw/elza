package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
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
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRecordWithCount;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
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
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param registerTypeIds   IDčka typu záznamu, může být null či prázdné (pak vrací vše)
     * @param parentRecordId    id rodiče, pokud je null načtou se všechny záznamy, jinak potomci daného rejstříku
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET)
    public RegRecordWithCount findRecord(@RequestParam(required = false) @Nullable final String search,
                                         @RequestParam final Integer from, @RequestParam final Integer count,
                                         @RequestParam(value = "registerTypeIds", required = false) @Nullable final Integer[] registerTypeIds,
                                         @RequestParam(required = false) @Nullable final Integer parentRecordId,
                                         @RequestParam(required = false) @Nullable final Integer versionId) {

        Set<Integer> registerTypeIdList = Collections.EMPTY_SET;
        if (registerTypeIds != null) {
            registerTypeIdList = new HashSet<>(Arrays.asList(registerTypeIds));
        }

        ArrFindingAid findingAid;
        if(versionId == null){
            findingAid = null;
        }else{
            ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze archivní pomůcky s id "+versionId);
            findingAid = version.getFindingAid();
        }

        RegRecord parentRecord = null;
        if(parentRecordId != null) {
            parentRecord = regRecordRepository.findOne(parentRecordId);
            Assert.notNull(parentRecord, "Nebylo nalezeno rejstříkové heslo s id " + parentRecordId);
        }

        final long foundRecordsCount = registryService.findRegRecordByTextAndTypeCount(search, registerTypeIdList,
                parentRecordId, findingAid);

        List<RegRecord> foundRecords = registryService
                .findRegRecordByTextAndType(search, registerTypeIdList, from, count, parentRecordId, findingAid);


        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(foundRecords);

        List<RegRecordVO> foundRecordVOList = factoryVo.createRegRecords(foundRecords, recordIdPartyIdMap, true);

        Map<Integer, RegRecordVO> parentRecordVOMap = new HashMap<>();
        for (RegRecordVO regRecordVO : foundRecordVOList) {
            parentRecordVOMap.put(regRecordVO.getRecordId(), regRecordVO);

        }

        if(CollectionUtils.isEmpty(registerTypeIdList)){
            for (RegRecordVO recordVO : foundRecordVOList) {
                factoryVo.fillRegisterTypeNamesToParents(recordVO, registerTypeIdList);
            }
        }

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
        RegRecordVO result = factoryVo.createRegRecord(record, partyId, false);
        result.setChilds(factoryVo.createRegRecords(childs, recordIdPartyIdMap, false));

        result.setVariantRecords(factoryVo.createRegVariantRecords(record.getVariantRecordList()));

        return result;
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRegisterTypeVO> getRecordTypes(){
          List<RegRegisterType> allTypes = registerTypeRepository.findAll();

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
        return factoryVo.createRegRecord(newRecordDO, recordParty == null ? null : recordParty.getPartyId(), false);
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

        ParParty recordParty = partyService.findParPartyByRecord(newRecordDO);
        return factoryVo.createRegRecord(newRecordDO, recordParty == null ? null : recordParty.getPartyId(), false);
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
    @RequestMapping(value = "/faScopes", method = RequestMethod.GET)
    public List<RegScopeVO> getScopeIdsByVersion(@RequestParam(required = false) @Nullable final Integer versionId) {

        ArrFindingAid findingAid;
        if (versionId == null) {
            findingAid = null;
        } else {
            ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze s id " + versionId);
            findingAid = version.getFindingAid();
        }

        Set<Integer> scopeIdsByFindingAid = registryService.getScopeIdsByFindingAid(findingAid);
        if (scopeIdsByFindingAid.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List<RegScopeVO> result = factoryVo.createScopes(scopeRepository.findAll(scopeIdsByFindingAid));
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


}
