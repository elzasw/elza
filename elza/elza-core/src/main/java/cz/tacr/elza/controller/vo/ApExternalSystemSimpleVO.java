package cz.tacr.elza.controller.vo;


import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;

/**
 * VO pro externí systém.
 *
 */
public class ApExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    private ApExternalSystemType type;

    public ApExternalSystemType getType() {
        return type;
    }

    public void setType(final ApExternalSystemType type) {
        this.type = type;
    }

    /**
     * Creates simple value object from AP external system.
     */
    public static ApExternalSystemSimpleVO newInstance(ApExternalSystem src) {
        ApExternalSystemSimpleVO vo = new ApExternalSystemSimpleVO();
        vo.setCode(src.getCode());
        vo.setId(src.getExternalSystemId());
        vo.setName(src.getName());
        vo.setType(src.getType());
        return vo;
    }
}
