package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.GisExternalSystem;

public class GisExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    protected GisExternalSystemSimpleVO(GisExternalSystem src) {
        super(src);
    }

    public static SysExternalSystemSimpleVO newInstance(GisExternalSystem extSystem) {
        return new GisExternalSystemSimpleVO(extSystem);
    }

}
