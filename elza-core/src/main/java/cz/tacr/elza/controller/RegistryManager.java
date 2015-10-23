package cz.tacr.elza.controller;

import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecRegister;
import cz.tacr.elza.repository.DescItemSpecRegisterRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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


    @RequestMapping(value = "/createRecord", method = RequestMethod.PUT)
    @Override
    @Transactional
    public RegRecord createRecord(@RequestBody final RegRecord record) {
        Assert.isNull(record.getRecordId(), "Při vytváření záznamu nesmí být vyplněno ID (recordId).");

        return saveRecordInternal(record);
    }

    @RequestMapping(value = "/updateRecord", method = RequestMethod.PUT)
    @Override
    public RegRecord updateRecord(@RequestBody final RegRecord record) {
        Assert.notNull(record.getRecordId(), "Očekáváno ID (recordId) pro update.");
        RegRecord recordTest = regRecordRepository.findOne(record.getRecordId());
        Assert.notNull(recordTest, "Nebyl nalezen záznam pro update s id " + record.getRecordId());

        saveRecordInternal(record);
        record.getVariantRecordList().forEach((variantRecord) -> {
            variantRecord.setRegRecord(null);
        });

        return record;
    }

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

        variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(recordId));
        partyRepository.delete(partyRepository.findParPartyByRecordId(recordId));

        regRecordRepository.delete(recordId);
    }

    @RequestMapping(value = "/createVariantRecord", method = RequestMethod.PUT)
    @Override
    public RegVariantRecord createVariantRecord(@RequestBody final RegVariantRecord variantRecord) {
        Assert.isNull(variantRecord.getVariantRecordId(), "Při vytváření záznamu nesmí být vyplněno ID (variantRecordId).");

        RegVariantRecord newVariantRecord = saveVariantRecordInternal(variantRecord);
        newVariantRecord.getRegRecord().getVariantRecordList();
//        newVariantRecord.setRegRecord(null);
        newVariantRecord.getRegRecord().setVariantRecordList(null);

        return newVariantRecord;
    }

    @RequestMapping(value = "/updateVariantRecord", method = RequestMethod.PUT)
    @Override
    public RegVariantRecord updateVariantRecord(@RequestBody final RegVariantRecord variantRecord) {
        Assert.notNull(variantRecord.getVariantRecordId(), "Očekáváno ID pro update.");
        RegVariantRecord variantRecordTest = variantRecordRepository.findOne(variantRecord.getVariantRecordId());
        Assert.notNull(variantRecordTest, "Nebyl nalezen záznam pro update s id " + variantRecord.getVariantRecordId());

        RegVariantRecord newVariantRecord = saveVariantRecordInternal(variantRecord);

        newVariantRecord.getRegRecord().setVariantRecordList(null);

        return newVariantRecord;
    }

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

    @Override
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRecord> findRecord(@RequestParam @Nullable final String search, @RequestParam final Integer from,
                                      @RequestParam final Integer count, 
                                      @RequestParam(value = "registerTypeIds") final Integer[] registerTypeIds) {
        List<Integer> registerTypeIdList = null;
        if (registerTypeIds != null) {
            registerTypeIdList = Arrays.asList(registerTypeIds);
        }
        List<RegRecord> regRecords = regRecordRepository.findRegRecordByTextAndType(search, registerTypeIdList, from, count);
        regRecords.forEach((record) -> {
            record.getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        });

        return regRecords;
    }

    @Override
    @RequestMapping(value = "/findRecordCount", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public long findRecordCount(@RequestParam @Nullable final String search,
                                @RequestParam(value = "registerTypeIds") Integer[] registerTypeIds) {

        List<Integer> registerTypeIdList = null;
        if (registerTypeIds != null) {
            registerTypeIdList = Arrays.asList(registerTypeIds);
        }
        return regRecordRepository.findRegRecordByTextAndTypeCount(search, registerTypeIdList);
    }

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

    /**
     * Uložení či update záznamu.
     *
     * @param record            naplněný objekt, bez vazeb
     * @param registerTypeId    id typu rejstříku
     * @param externalSourceId  id externího zdroje, může být null
     * @return      výslendný objekt
     */
    @Transactional
    private RegRecord saveRecordInternal(final RegRecord record) {
        Assert.notNull(record);

        Assert.notNull(record.getRecord(), "Není vyplněné Record.");
        Assert.notNull(record.getCharacteristics(), "Není vyplněné Characteristics.");
        Assert.notNull(record.getLocal(), "Není vyplněné Local.");

        RegRegisterType regRegisterType = record.getRegisterType();
        Assert.notNull(regRegisterType, "Není vyplněné RegisterType.");
        Integer registerTypeId = regRegisterType.getRegisterTypeId();
        Assert.notNull(registerTypeId, "RegisterType nemá vyplněné ID.");
        regRegisterType = registerTypeRepository.findOne(registerTypeId);
        Assert.notNull(regRegisterType, "RegisterType nebylo nalezeno podle id " + registerTypeId);
        record.setRegisterType(regRegisterType);

        RegExternalSource externalSource = record.getExternalSource();
        if (externalSource != null) {
            Integer externalSourceId = externalSource.getExternalSourceId();
            Assert.notNull(externalSourceId, "ExternalSource nemá vyplněné ID.");
            externalSource = externalSourceRepository.findOne(externalSourceId);
            Assert.notNull(externalSource, "ExternalSource nebylo nalezeno podle id " + externalSourceId);
            record.setExternalSource(externalSource);
        }

        return regRecordRepository.save(record);
    }

    /**
     * Uložení či update variantního záznamu.
     *
     * @param variantRecord     variantní záznam, bez vazeb
     * @param regRecordId       id záznamu rejstříku
     * @return      výslendný objekt uložený do db
     */
    @Transactional
    private RegVariantRecord saveVariantRecordInternal(final RegVariantRecord variantRecord) {
        Assert.notNull(variantRecord);

        RegRecord regRecord = variantRecord.getRegRecord();
        Assert.notNull(regRecord, "RegRecord musí být vyplněno.");
        Integer recordId = regRecord.getRecordId();
        Assert.notNull(recordId, "RegRecord nemá vyplněno ID.");

        regRecord = regRecordRepository.findOne(recordId);
        Assert.notNull(regRecord, "RegRecord nebylo nalezeno podle id " + recordId);
        variantRecord.setRegRecord(regRecord);

        return variantRecordRepository.save(variantRecord);
    }

}
