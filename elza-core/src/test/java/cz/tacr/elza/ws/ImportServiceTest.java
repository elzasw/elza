package cz.tacr.elza.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.junit.Test;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApPartVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.ws.core.v1.ImportService;
import cz.tacr.elza.ws.types.v1.ImportRequest;
import io.restassured.RestAssured;

public class ImportServiceTest extends AbstractControllerTest {

    public final String SYSTEM_CODE = "TESTSYSTEM";

    ImportService createImportServiceClient() {
        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.IMPORT_SERVICE_URL;
        ImportService importServiceClient = WebServiceClientFactory.createImportService(address, "admin", "admin");
        return importServiceClient;
    }

    @Test
    public void importEntityTest() throws IOException, ApiException {
        // create external system
        ApExternalSystemVO externalSystemVO = new ApExternalSystemVO();
        externalSystemVO.setCode(SYSTEM_CODE);
        externalSystemVO.setName(SYSTEM_CODE);
        externalSystemVO.setUrl("camurl");
        externalSystemVO.setApiKeyId("apikey");
        externalSystemVO.setApiKeyValue("apikeyvalue");
        externalSystemVO.setType(ApExternalSystemType.CAM);
        SysExternalSystemVO externalSystemCreatedVO = createExternalSystem(externalSystemVO);
        assertNotNull(externalSystemCreatedVO.getId());

        ImportService impService = createImportServiceClient();

        // prepare import request
        File file = getFile("cam/3392.xml");
        DataSource ds = new ByteArrayDataSource(Files.newInputStream(file.toPath()), "application/octet-stream");
        DataHandler dh = new DataHandler(ds);

        ImportRequest importReq = WSRequestFactory.createImportRequest(SYSTEM_CODE, SchemaManager.CAM_SCHEMA_URL);
        importReq.setDisposition(WSRequestFactory.createDisposition(SCOPE_GLOBAL));
        importReq.setBinData(dh);
        impService.importData(importReq);

        // delete all parts (except pref. name)
        ApAccessPointVO apvo = this.getAccessPoint("7278d973-f0a0-4002-a5a4-5ca176e6db6c");
        assertNotNull(apvo);
        for(ApPartVO part:apvo.getParts()) {
            if(!part.getId().equals(apvo.getPreferredPart())) {
                deletePart(apvo.getId(), part.getId());
            }
        }
        
        // run reimport
        importReq = WSRequestFactory.createImportRequest(SYSTEM_CODE, SchemaManager.CAM_SCHEMA_URL);
        importReq.setDisposition(WSRequestFactory.createDisposition(SCOPE_GLOBAL));
        importReq.setBinData(dh);
        impService.importData(importReq);

        ApAccessPointVO apvo2 = this.getAccessPoint("7278d973-f0a0-4002-a5a4-5ca176e6db6c");
        assertNotNull(apvo2);

        assertTrue(apvo2.getParts().size() == apvo.getParts().size());

        // delete entity
        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        accesspointsApi.accessPointDeleteAccessPoint(apvo.getId().toString(), deleteAPDetail);

        // run reimport
        importReq = WSRequestFactory.createImportRequest(SYSTEM_CODE, SchemaManager.CAM_SCHEMA_URL);
        importReq.setDisposition(WSRequestFactory.createDisposition(SCOPE_GLOBAL));
        importReq.setBinData(dh);
        impService.importData(importReq);

        ApAccessPointVO apvo3 = this.getAccessPoint("7278d973-f0a0-4002-a5a4-5ca176e6db6c");
        assertNotNull(apvo3);
    }
}
