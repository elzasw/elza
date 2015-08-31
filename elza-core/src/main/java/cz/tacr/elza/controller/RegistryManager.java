package cz.tacr.elza.controller;

import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
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


    @RequestMapping(value = "/createRecord", method = RequestMethod.PUT)
    @Transactional
    public @ResponseBody RegRecord createRecord(@RequestBody final RegRecord record,
                                                @RequestParam final Integer registerTypeId,
                                                @RequestParam @Nullable final Integer externalSourceId) {

        Assert.notNull(record);
        Assert.notNull(registerTypeId);
        Assert.notNull(record.getRecord());
        Assert.notNull(record.getCharacteristics());
        Assert.notNull(record.getLocal());

        RegRegisterType regRegisterType = registerTypeRepository.getOne(registerTypeId);
        record.setRegisterType(regRegisterType);

        if (externalSourceId != null) {
            RegExternalSource externalSource = externalSourceRepository.getOne(externalSourceId);
            record.setExternalSource(externalSource);
        }

        return regRecordRepository.save(record);
    }

    @RequestMapping(value = "/updateRecord", method = RequestMethod.PUT)
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
        });

        return regRecords;
    }

    @Override
    @RequestMapping(value = "/findRecordCount", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public long findRecordCount(@RequestParam @Nullable final String search,
                                           @RequestParam final Integer registerTypeId) {

        return regRecordRepository.findRegRecordByTextAndTypeCount(search, registerTypeId);
    }

    @Override
    @RequestMapping(value = "/getRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"recordId"})
    @Transactional
    public RegRecord getRecord(@RequestParam(value = "recordId") final Integer recordId) {
        Assert.notNull(recordId);

        return regRecordRepository.getOne(recordId);
    }

}
