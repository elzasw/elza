package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ConfigClientVOService;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRecordWithCount;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
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
    private ConfigClientVOService factoryVo;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param registerTypeIds   IDčka typu záznamu, může být null či prázdné (pak vrací vše)
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegRecordWithCount findRecord(@RequestParam(required = false) @Nullable final String search,
                                         @RequestParam final Integer from, @RequestParam final Integer count,
                                         @RequestParam(value = "registerTypeIds", required = false) @Nullable final Integer[] registerTypeIds) {

        List<Integer> registerTypeIdList = null;
        if (registerTypeIds != null) {
            registerTypeIdList = Arrays.asList(registerTypeIds);
        }

        final boolean onlyLocal = false;


        List<RegRecord> regRecords = registryService
                .findRegRecordByTextAndType(search, registerTypeIdList, onlyLocal, from, count);
        List<ParParty> recordParties = partyService.findParPartyByRecords(regRecords);

        List<RegRecordVO> recordVOList = factoryVo
                .createRegRecords(regRecords, PartyUtils.createRecordPartyMap(recordParties));

        long countAll = registryService.findRegRecordByTextAndTypeCount(search, registerTypeIdList, onlyLocal);

        return new RegRecordWithCount(recordVOList, countAll);
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
        RegRecordVO result = factoryVo.createRegRecord(record, recordParty == null ? null : recordParty.getPartyId());
        result.setChilds(factoryVo.createRegRecords(childs, recordPartyMap));

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


}
