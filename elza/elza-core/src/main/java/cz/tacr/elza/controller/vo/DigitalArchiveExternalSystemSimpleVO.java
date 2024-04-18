package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.DigitalArchiveExternalSystem;

public class DigitalArchiveExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    protected DigitalArchiveExternalSystemSimpleVO(DigitalArchiveExternalSystem src) {
        super(src);
    }

    public static SysExternalSystemSimpleVO newInstance(DigitalArchiveExternalSystem extSystem) {
        return new DigitalArchiveExternalSystemSimpleVO(extSystem);
    }

}
