package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
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
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.PartyUtils;


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

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param registerTypeIds   IDčka typu záznamu, může být null či prázdné (pak vrací vše)
     * @param parentRecordId    id rodiče, pokud je null načtou se kořenové záznamy, jinak potomci daného rejstříku
     *
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET)
    public RegRecordWithCount findRecord(@RequestParam(required = false) @Nullable final String search,
                                         @RequestParam final Integer from, @RequestParam final Integer count,
                                         @RequestParam(value = "registerTypeIds", required = false) @Nullable final Integer[] registerTypeIds,
                                         @RequestParam(required = false) @Nullable final Integer parentRecordId) {

        List<Integer> registerTypeIdList = null;
        if (registerTypeIds != null) {
            registerTypeIdList = Arrays.asList(registerTypeIds);
        }

        final boolean onlyLocal = false;

        // všechny záznamy
        List<RegRecord> allRecords = new LinkedList<>();

        List<RegRecord> regRecords = registryService
                .findRegRecordByTextAndType(search, registerTypeIdList, onlyLocal, from, count, parentRecordId);
        allRecords.addAll(regRecords);

        // děti
        Map<Integer, List<RegRecord>> parentChildrenMap = new HashMap<>();
        for (RegRecord regRecord : regRecords) {
            List<RegRecord> children = regRecordRepository.findByParentRecord(regRecord);
            allRecords.addAll(children);
            parentChildrenMap.put(regRecord.getRecordId(), children);
        }

        List<ParParty> recordParties = partyService.findParPartyByRecords(allRecords);

        Map<Integer, ParParty> recordPartyMap = PartyUtils.createRecordPartyMap(recordParties);
        List<RegRecordVO> parentRecordVOList = factoryVo.createRegRecords(regRecords, recordPartyMap, true);

        Map<Integer, RegRecordVO> parentRecordVOMap = new HashMap<>();
        for (RegRecordVO regRecordVO : parentRecordVOList) {
            parentRecordVOMap.put(regRecordVO.getRecordId(), regRecordVO);
        }

        for (Integer recordId : parentChildrenMap.keySet()) {
            List<RegRecord> children = parentChildrenMap.get(recordId);

            List<RegRecordVO> childrenVO = new ArrayList<RegRecordVO>(children.size());
            parentRecordVOMap.get(recordId).setChilds(childrenVO);
            for (RegRecord child : children) {
                ParParty parParty = recordPartyMap.get(child.getRecordId());
                Integer partyId = parParty == null ? null : parParty.getPartyId();
                RegRecordVO regRecordVO = factoryVo.createRegRecord(child, partyId, true);
                childrenVO.add(regRecordVO);

                List<RegRecord> childChildren = regRecordRepository.findByParentRecord(child);
                regRecordVO.setHasChildren(childChildren.isEmpty() ? false : true);
            }
        }

        long countAll = registryService.findRegRecordByTextAndTypeCount(search, registerTypeIdList, onlyLocal, parentRecordId);

        return new RegRecordWithCount(parentRecordVOList, countAll);
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


        Map<Integer, ParParty> recordPartyMap = PartyUtils
                .createRecordPartyMap(partyService.findParPartyByRecords(records));

        ParParty recordParty = recordPartyMap.get(recordId);
        RegRecordVO result = factoryVo.createRegRecord(record, recordParty == null ? null : recordParty.getPartyId(), false);
        result.setChilds(factoryVo.createRegRecords(childs, recordPartyMap, false));

        result.setVariantRecords(factoryVo.createRegVariantRecords(record.getVariantRecordList()));

        return result;
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/getRecordTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRegisterTypeVO> getRecordTypes(){
          List<RegRegisterType> allTypes = registerTypeRepository.findAll();

          return factoryVo.createRegisterTypesTree(allTypes);
    }

    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     * @return vytvořený záznam
     */
    @RequestMapping(value = "/createRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegRecordVO createRecord(@RequestBody final RegRecordVO record) {
        Assert.isNull(record.getRecordId(), "Při vytváření záznamu nesmí být vyplněno ID (recordId).");

        RegRecord recordDO = factoryDO.createRegRecord(record);
        RegRecord newRecordDO = registryService.saveRecord(recordDO, true);

        ParParty recordParty = partyService.findParPartyByRecord(newRecordDO);
        return factoryVo.createRegRecord(newRecordDO, recordParty == null ? null : recordParty.getPartyId(), false);
    }

    /**
     * Aktualizace rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     * @return aktualizovaný záznam
     */
    @RequestMapping(value = "/updateRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegRecordVO updateRecord(@RequestBody final RegRecordVO record) {
        Assert.notNull(record.getRecordId(), "Očekáváno ID (recordId) pro update.");
        RegRecord recordTest = regRecordRepository.findOne(record.getRecordId());
        Assert.notNull(recordTest, "Nebyl nalezen záznam pro update s id " + record.getRecordId());

        RegRecord recordDO = factoryDO.createRegRecord(record);
        RegRecord newRecordDO = registryService.saveRecord(recordDO, true);

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

}
