package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cz.tacr.elza.api.DigitalRepositoryType;
import org.junit.jupiter.api.TestInstance;

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
 * <p>Uses per-class lifecycle: base {@code setUp} / {@code tearDown} run once
 * per class. Tests do not clean up the entities they create — they accumulate
 * through the class run and are wiped by the next class's
 * {@code @BeforeEach deleteTables()}. Tests that inspect entity counts capture
 * a baseline at their start rather than assuming an empty DB.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminControllerTest extends AbstractControllerTest {

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    /**
     * Triggers the admin {@code /reindex} endpoint — no response-body check,
     * just verifies the endpoint returns successfully.
     *
     * <p><b>Creates:</b> nothing.
     * <br><b>Cleans up:</b> n/a.
     */
    @Test
    public void reindexTest() {
        get(REINDEX);
    }

    /**
     * Verifies {@code /reindex/status} returns a non-null boolean payload.
     *
     * <p><b>Creates:</b> nothing.
     * <br><b>Cleans up:</b> n/a.
     */
    @Test
    public void reindexStatusTest() {
        Response response = get(REINDEX_STATUS);
        Boolean status = response.getBody().as(Boolean.class);
        assertNotNull(status);
    }

    /**
     * Triggers the admin {@code /cache/reset} endpoint.
     *
     * <p><b>Creates:</b> nothing.
     * <br><b>Cleans up:</b> n/a.
     */
    @Test
    public void cacheReset() {
        get(CACHE_RESET);
    }

    /**
     * Creates three external systems (digital repository, digitization
     * frontdesk, CAM AP system), updates one, deletes one, and verifies the
     * counts move accordingly. Uses baseline-relative counts so the test
     * tolerates siblings having left state in the shared DB.
     *
     * <p><b>Creates:</b> 3 external systems (codes TST1/TST2/TST3).
     * <br><b>Cleans up:</b> deletes the first one (2 remain — intentional, see
     * class javadoc).
     */
    @Test
    public void externalSystems() {
        int baselineSize = getExternalSystems().size();

        ArrDigitalRepositoryVO digitalRepositoryVO = new ArrDigitalRepositoryVO();
        digitalRepositoryVO.setCode("TST1");
        digitalRepositoryVO.setName("Test 1");
        digitalRepositoryVO.setSendNotification(true);
        digitalRepositoryVO.setDigitalRepositoryType(DigitalRepositoryType.FILESYSTEM);
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
        externalSystemVO.setScopeId(1);

        SysExternalSystemVO externalSystemCreatedVO = createExternalSystem(externalSystemVO);
        assertNotNull(externalSystemCreatedVO.getId());

        List<SysExternalSystemVO> externalSystems = getExternalSystems();
        assertEquals(baselineSize + 3, externalSystems.size());

        ((ArrDigitalRepositoryVO) digitalRepositoryCreatedVO).setSendNotification(false);
        SysExternalSystemVO digitalRepositoryUpdatedVO = updateExternalSystem(digitalRepositoryCreatedVO);
        assertTrue(!((ArrDigitalRepositoryVO) digitalRepositoryUpdatedVO).getSendNotification());

        deleteExternalSystem(externalSystems.get(0));

        externalSystems = getExternalSystems();
        assertEquals(baselineSize + 2, externalSystems.size());
    }
}
