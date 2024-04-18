package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.StorageExternalSystem;

public class StorageExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    protected StorageExternalSystemSimpleVO(StorageExternalSystem src) {
        super(src);
    }

    public static SysExternalSystemSimpleVO newInstance(StorageExternalSystem extSystem) {
        return new StorageExternalSystemSimpleVO(extSystem);
    }

}
