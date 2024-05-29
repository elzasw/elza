package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositoryVO;
import cz.tacr.elza.controller.vo.ArrDigitizationFrontdeskVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import io.restassured.response.Response;


/**
 * Testování metod z AdminController.
 *
 */
public class AdminControllerTest extends AbstractControllerTest {

    @Test
    public void reindexTest() {
        get(REINDEX);
    }

    @Test
    public void reindexStatusTest() {
        Response response = get(REINDEX_STATUS);
        Boolean status = response.getBody().as(Boolean.class);
        assertNotNull(status);
    }

    @Test
    public void cacheReset() {
        get(CACHE_RESET);
    }

    @Test
    public void externalSystems() {
        List<SysExternalSystemVO> externalSystems = getExternalSystems();
        assertTrue(externalSystems.size() == 0);

        ArrDigitalRepositoryVO digitalRepositoryVO = new ArrDigitalRepositoryVO();
        digitalRepositoryVO.setCode("TST1");
        digitalRepositoryVO.setName("Test 1");
        digitalRepositoryVO.setSendNotification(true);
        SysExternalSystemVO digitalRepositoryCreatedVO = createExternalSystem(digitalRepositoryVO);
        assertNotNull(digitalRepositoryCreatedVO.getId());

        ArrDigitizationFrontdeskVO digitizationFrontdeskVO = new ArrDigitizationFrontdeskVO();
        digitizationFrontdeskVO.setCode("TST2");
        digitizationFrontdeskVO.setName("Test 2");
        SysExternalSystemVO digitizationFrontdeskCreatedVO = createExternalSystem(digitizationFrontdeskVO);
        assertNotNull(digitizationFrontdeskCreatedVO.getId());

        ApExternalSystemVO externalSystemVO = new ApExternalSystemVO();
        externalSystemVO.setCode("TST3");
        externalSystemVO.setName("Test 3");
        externalSystemVO.setType(ApExternalSystemType.CAM);

        SysExternalSystemVO externalSystemCreatedVO = createExternalSystem(externalSystemVO);
        assertNotNull(externalSystemCreatedVO.getId());

        externalSystems = getExternalSystems();
        assertTrue(externalSystems.size() == 3);

        ((ArrDigitalRepositoryVO) digitalRepositoryCreatedVO).setSendNotification(false);
        SysExternalSystemVO digitalRepositoryUpdatedVO = updateExternalSystem(digitalRepositoryCreatedVO);
        assertTrue(!((ArrDigitalRepositoryVO) digitalRepositoryUpdatedVO).getSendNotification());

        deleteExternalSystem(externalSystems.get(0));

        externalSystems = getExternalSystems();
        assertTrue(externalSystems.size() == 2);
    }
}
