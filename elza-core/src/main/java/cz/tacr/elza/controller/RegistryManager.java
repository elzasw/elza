package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecRegister;
import cz.tacr.elza.domain.vo.RegRecordWithCount;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DescItemSpecRegisterRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.RegistryService;

/**
 * Implementace Api pro práci s rejstříkem.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@RestController
@RequestMapping("/api/registryManager")
public class RegistryManager implements cz.tacr.elza.api.controller.RegistryManager<RegRecord, RegVariantRecord> {

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private DescItemSpecRegisterRepository descItemSpecRegisterRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private ExternalSourceRepository externalSourceRepository;

    @Autowired
    private PartyManager partyManager;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private RegistryService registryService;


    //přepsáno do RegistryController
    @RequestMapping(value = "/createRecord", method = RequestMethod.PUT)
    @Override
    @Transactional
    public RegRecord createRecord(@RequestBody final RegRecord record) {
        Assert.isNull(record.getRecordId(), "Při vytváření záznamu nesmí být vyplněno ID (recordId).");

        return registryService.saveRecord(record, true);
    }

    //přepsáno do RegistryController
    @RequestMapping(value = "/updateRecord", method = RequestMethod.PUT)
    @Override
    public RegRecord updateRecord(@RequestBody final RegRecord record) {
        Assert.notNull(record.getRecordId(), "Očekáváno ID (recordId) pro update.");
        RegRecord recordTest = regRecordRepository.findOne(record.getRecordId());
        Assert.notNull(recordTest, "Nebyl nalezen záznam pro update s id " + record.getRecordId());

        registryService.saveRecord(record, true);
        record.getVariantRecordList().forEach((variantRecord) -> {
            variantRecord.setRegRecord(null);
        });

        return record;
    }

    //přepsáno do RegistryController
    @Override
    @RequestMapping(value = "/deleteRecord", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"recordId"})
    @Transactional
    public void deleteRecord(@RequestParam(value = "recordId") final Integer recordId) {
        Assert.notNull(recordId);
        RegRecord record = regRecordRepository.findOne(recordId);
        if (record == null) {
            return;
        }

        registryService.deleteRecord(record, true);
    }


    //přepsáno do RegistryController
    @RequestMapping(value = "/createVariantRecord", method = RequestMethod.PUT)
    @Override
    public RegVariantRecord createVariantRecord(@RequestBody final RegVariantRecord variantRecord) {
        Assert.isNull(variantRecord.getVariantRecordId(), "Při vytváření záznamu nesmí být vyplněno ID (variantRecordId).");

        RegVariantRecord newVariantRecord = registryService.saveVariantRecord(variantRecord);
        newVariantRecord.getRegRecord().getVariantRecordList();
//        newVariantRecord.setRegRecord(null);
        newVariantRecord.getRegRecord().setVariantRecordList(null);

        return newVariantRecord;
    }

    //přepsáno do RegistryController
    @RequestMapping(value = "/updateVariantRecord", method = RequestMethod.PUT)
    @Override
    public RegVariantRecord updateVariantRecord(@RequestBody final RegVariantRecord variantRecord) {
        Assert.notNull(variantRecord.getVariantRecordId(), "Očekáváno ID pro update.");
        RegVariantRecord variantRecordTest = variantRecordRepository.findOne(variantRecord.getVariantRecordId());
        Assert.notNull(variantRecordTest, "Nebyl nalezen záznam pro update s id " + variantRecord.getVariantRecordId());

        RegVariantRecord newVariantRecord = registryService.saveVariantRecord(variantRecord);

        newVariantRecord.getRegRecord().setVariantRecordList(null);

        return newVariantRecord;
    }

    //přepsáno do RegistryController
    @Override
    @RequestMapping(value = "/deleteVariantRecord", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"variantRecordId"})
    @Transactional
    public void deleteVariantRecord(@RequestParam(value = "variantRecordId") final Integer variantRecordId) {
        Assert.notNull(variantRecordId);
        RegVariantRecord variantRecord = variantRecordRepository.findOne(variantRecordId);
        if (variantRecord == null) {
            return;
        }

        variantRecordRepository.delete(variantRecordId);
    }

    @Override
    @RequestMapping(value = "/getRegisterTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRegisterType> getRegisterTypes() {
        return registerTypeRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getRegisterTypesForDescItemSpec", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRegisterType> getRegisterTypesForDescItemSpec(@RequestParam final Integer descItemSpecId) {
        RulDescItemSpec descItemSpec =  descItemSpecRepository.findOne(descItemSpecId);
        Set<RegRegisterType> registerSet = new HashSet<>();
        List<RulDescItemSpecRegister> disRegisterList = descItemSpecRegisterRepository.findByDescItemSpecId(descItemSpec);
        for (RulDescItemSpecRegister rulDescItemSpecRegister : disRegisterList) {
            registerSet.add(rulDescItemSpecRegister.getRegisterType());
        }
        return new LinkedList<>(registerSet);
    }

    @Override
    @RequestMapping(value = "/getExternalSources", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegExternalSource> getExternalSources() {
        return externalSourceRepository.findAll();
    }


    //přepsáno do RecordController
    @Override
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RegRecordWithCount findRecord(@RequestParam(required = false) @Nullable final String search,
                 @RequestParam final Integer from, @RequestParam final Integer count,
                 @RequestParam(value = "registerTypeIds", required = false) @Nullable final Integer[] registerTypeIds) {

        List<Integer> registerTypeIdList = null;
        if (registerTypeIds != null) {
            registerTypeIdList = Arrays.asList(registerTypeIds);
        }

        List<RegRecord> regRecords = registryService.findRegRecordByTextAndType(search, registerTypeIdList, from, count,null, null);
        regRecords.forEach((record) -> {
            record.getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        });

        long countAll = regRecordRepository
                .findRegRecordByTextAndTypeCount(search, registerTypeIdList, null, null);

        return new RegRecordWithCount(regRecords, countAll);
    }

    //přepsáno do RecordController
    @Override
    @RequestMapping(value = "/getRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"recordId"})
    public RegRecord getRecord(@RequestParam(value = "recordId") final Integer recordId) {
        Assert.notNull(recordId);

        RegRecord record = regRecordRepository.getOne(recordId);
        record.getVariantRecordList().forEach((variantRecord) -> {
            variantRecord.setRegRecord(null);
        });

        return record;
    }
}
