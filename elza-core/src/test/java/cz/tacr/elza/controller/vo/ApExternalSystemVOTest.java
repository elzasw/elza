package cz.tacr.elza.controller.vo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;

public class ApExternalSystemVOTest {

    private static ApExternalSystemVO createVO() {
        ApExternalSystemVO vo = new ApExternalSystemVO();
        vo.setCode("CAM");
        vo.setName("CAM");
        vo.setUrl("https://cam.example");
        vo.setType(ApExternalSystemType.CAM_V2);
        return vo;
    }

    @Test
    void createEntity_tlsVerifyDefaultsToTrue() {
        ApExternalSystem entity = (ApExternalSystem) createVO().createEntity(new ApScope());

        assertTrue(entity.getTlsVerify());
    }

    @Test
    void createEntity_keepsSkippedTlsVerification() {
        ApExternalSystemVO vo = createVO();
        vo.setTlsVerify(false);

        ApExternalSystem entity = (ApExternalSystem) vo.createEntity(new ApScope());

        assertFalse(entity.getTlsVerify());
        assertFalse(ApExternalSystemVO.newInstance(entity).getTlsVerify());
    }
}
