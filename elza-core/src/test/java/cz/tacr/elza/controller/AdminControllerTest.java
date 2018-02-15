package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositoryVO;
import cz.tacr.elza.controller.vo.ArrDigitizationFrontdeskVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;


/**
 * Testování metod z AdminController.
 *
 * @author Martin Šlapa
 * @since 16.2.2016
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
        Assert.notNull(status);
    }

    @Test
    public void cacheReset() {
        get(CACHE_RESET);
    }

    @Test
    public void externalSystems() {
        List<SysExternalSystemVO> externalSystems = getExternalSystems();
        Assert.isTrue(externalSystems.size() == 0, "Počet externích systémů musí být 0. " + externalSystems);
        ArrDigitalRepositoryVO digitalRepositoryVO = new ArrDigitalRepositoryVO();
        digitalRepositoryVO.setCode("TST1");
        digitalRepositoryVO.setName("Test 1");
        digitalRepositoryVO.setSendNotification(true);

        SysExternalSystemVO digitalRepositoryCreatedVO = createExternalSystem(digitalRepositoryVO);
        Assert.notNull(digitalRepositoryCreatedVO.getId());

        ArrDigitizationFrontdeskVO digitizationFrontdeskVO = new ArrDigitizationFrontdeskVO();
        digitizationFrontdeskVO.setCode("TST2");
        digitizationFrontdeskVO.setName("Test 2");
        SysExternalSystemVO digitizationFrontdeskCreatedVO = createExternalSystem(digitizationFrontdeskVO);
        Assert.notNull(digitizationFrontdeskCreatedVO.getId());

        ApExternalSystemVO externalSystemVO = new ApExternalSystemVO();
        externalSystemVO.setCode("TST3");
        externalSystemVO.setName("Test 3");
        externalSystemVO.setType(ApExternalSystemType.INTERPI);

        SysExternalSystemVO externalSystemCreatedVO = createExternalSystem(externalSystemVO);
        Assert.notNull(externalSystemCreatedVO.getId());

        externalSystems = getExternalSystems();
        Assert.isTrue(externalSystems.size() == 3,  "Počet externích systémů musí být 3. " + externalSystems);

        ((ArrDigitalRepositoryVO) digitalRepositoryCreatedVO).setSendNotification(false);
        SysExternalSystemVO digitalRepositoryUpdatedVO = updateExternalSystem(digitalRepositoryCreatedVO);
        Assert.isTrue(!((ArrDigitalRepositoryVO) digitalRepositoryUpdatedVO).getSendNotification());

        deleteExternalSystem(externalSystems.get(0));

        externalSystems = getExternalSystems();
        Assert.isTrue(externalSystems.size() == 2,  "Počet externích systémů musí být 2. " + externalSystems);
    }

}
