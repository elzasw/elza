package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.AiExternalSystem;

public class AiExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    protected AiExternalSystemSimpleVO(AiExternalSystem src) {
        super(src);
    }

    public static SysExternalSystemSimpleVO newInstance(AiExternalSystem extSystem) {
        return new AiExternalSystemSimpleVO(extSystem);
    }

}
