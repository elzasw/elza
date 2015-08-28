package cz.tacr.elza.controller;

import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Rejstřík.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@RestController
@RequestMapping("/api/registryManager")
public class RegistryManager implements cz.tacr.elza.api.controller.RegistryManager<RegRecord> {

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private AbstractPartyRepository abstractPartyRepository;

    @Autowired
    private ExternalSourceRepository externalSourceRepository;


    @RequestMapping(value = "/createRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public @ResponseBody RegRecord createRecord(@RequestBody final RegRecord regRecord) {
        Assert.notNull(regRecord);
        Assert.notNull(regRecord.getRegisterType());
        Assert.notNull(regRecord.getRecord());
        Assert.notNull(regRecord.getCharacteristics());
        Assert.notNull(regRecord.getLocal());

        return regRecordRepository.save(regRecord);
    }

    @RequestMapping(value = "/updateRecord", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public RegRecord updateRecord(@RequestBody final RegRecord record) {
        Assert.notNull(record);
        Assert.notNull(record.getRegisterType());
        Assert.notNull(record.getRecord());
        Assert.notNull(record.getCharacteristics());
        Assert.notNull(record.getLocal());

        return regRecordRepository.save(record);
    }

    @Override
    @RequestMapping(value = "/deleteRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"recordId"})
    @Transactional
    public void deleteRecord(@RequestParam(value = "recordId") final Integer recordId) {
        Assert.notNull(recordId);

        variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(recordId));
        abstractPartyRepository.delete(abstractPartyRepository.findParAbstractPartyByRecordId(recordId));

        regRecordRepository.delete(recordId);
    }

    @Override
    @RequestMapping(value = "/deleteVariantRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"variantRecordId"})
    @Transactional
    public void deleteVariantRecord(@RequestParam(value = "variantRecordId") final Integer variantRecordId) {
        Assert.notNull(variantRecordId);

        variantRecordRepository.delete(variantRecordId);
    }

    @Override
    @RequestMapping(value = "/getRegisterTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRegisterType> getRegisterTypes() {
        return registerTypeRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getExternalSources", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegExternalSource> getExternalSources() {
        return externalSourceRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/findRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegRecord> findRecord(@RequestParam @Nullable final String search, @RequestParam final Integer from,
                                      @RequestParam final Integer count, @RequestParam final Integer registerTypeId) {

        List<RegRecord> regRecords = regRecordRepository.findRegRecordByTextAndType(search, registerTypeId, from, count);
        regRecords.forEach((record) -> {
            record.getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
//            RegRecordExt regRecordExt = new RegRecordExt();
//            BeanUtils.copyProperties(record, regRecordExt);

        });


        return regRecords;
    }

//    @Override
//    @RequestMapping(value = "/getRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
//            params = {"recordId"})
//    @Transactional
//    public RegRecord getRecord(@RequestParam(value = "recordId") final Integer recordId) {
//
//    }

    @Override
    @RequestMapping(value = "/getVariantRecords", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RegVariantRecord> getVariantRecords() {
        return variantRecordRepository.findAll();
    }


}
