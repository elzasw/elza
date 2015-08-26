package cz.tacr.elza.controller;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;

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

//    @RequestMapping(value = "/updateRecord", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
//            params = {"record"}, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional
//    public RegRecord updateRecord(@RequestParam(value = "record") final RegRecord record) {
//        Assert.notNull(record);
//        Assert.notNull(record.getRegisterType());
//        Assert.notNull(record.getRecord());
//        Assert.notNull(record.getCharacteristics());
//        Assert.notNull(record.getLocal());
//
//        List<RegRecord> before = regRecordRepository.findAll();
//
//        RegRecord result = regRecordRepository.save(record);
//
//        List<RegRecord> after = regRecordRepository.findAll();
//
//        return result;
//    }
//
//    void deleteRecord(Integer recordId) {
//        Assert.notNull(recordId);
//                                     //TODO
////        regRecordRepository.delete();
//    }



}
