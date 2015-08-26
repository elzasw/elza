package cz.tacr.elza.controller;

import com.jayway.restassured.RestAssured;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.repository.RegisterTypeRepository;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegistryManagerTest extends AbstractRestTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CREATE_RECORD_URL = REGISTRY_MANAGER_URL + "/createRecord";

    private static final String RECORD_ATT = "regRecord";


    @Autowired
    private RegistryManager registryManager;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

//    private Integer id;

//    @Test
//    @Transactional
//    public void testCreateRecord() throws Exception {
//        RegRecord regRecord = new RegRecord();
//        regRecord.setRecord("TEST RECORD");
//        regRecord.setCharacteristics("TEST CHAR");
//        regRecord.setLocal(false);
//
//        regRecord = registryManager.createRecord(regRecord);
//        id = regRecord.getId();
//    }

    @Test
    public void testRestCreateRecord() throws Exception {
        RegRecord record = createRecordRest();

        Assert.assertNotNull(record);
        Assert.assertNotNull(record.getRecordId());
    }

//    @Test
//    @Transactional
//    public void testUpdateRecord() throws Exception {
//
//        RegRecord regRecord2 = new RegRecord();
//        regRecord2.setRecordId(id);
//        regRecord2.setRecord("TEST RECORD 2");
//        regRecord2.setCharacteristics("TEST CHAR 2");
//        regRecord2.setLocal(true);
//
//        List<RegRegisterType> all = registerTypeRepository.findAll();
//        regRecord2.setRegisterType(all.get(0));
//
//        registryManager.updateRecord(regRecord2);
//        return;
//    }

    private RegRecord createRecordRest() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);
        regRecord.setRegisterType(createRegisterType());

//        try {
//            new ObjectMapper().writeValue(System.out, regRecord);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        RestTemplate template = new RestTemplate();
        RequestEntity<RegRecord> requestEntity = null;
        try {
            requestEntity = RequestEntity.put(new URI("http://localhost:" + RestAssured.port + "/elza"  + CREATE_RECORD_URL)).accept(MediaType.APPLICATION_JSON).body(regRecord);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ResponseEntity<RegRecord> responseEntity = template.exchange(requestEntity, RegRecord.class);


//        Response response =
//                given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).body(regRecord)
//                        .put(CREATE_RECORD_URL);
//        logger.info(response.asString());
//        Assert.assertEquals(200, response.statusCode());

//        RegRecord record = response.getBody().as(RegRecord.class);

        RegRecord record = requestEntity.getBody();

        return record;
    }

    protected RegRegisterType createRegisterType() {
        RegRegisterType regRegisterType = new RegRegisterType();
        regRegisterType.setCode(TEST_CODE);
        regRegisterType.setName(TEST_NAME);
        registerTypeRepository.save(regRegisterType);
        return regRegisterType;
    }

}
