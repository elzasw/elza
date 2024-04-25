package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.DaRemoteRepository;

public class DaRemoteRepositorySimpleVO extends SysExternalSystemSimpleVO {

    protected DaRemoteRepositorySimpleVO(DaRemoteRepository src) {
        super(src);
    }

    public static SysExternalSystemSimpleVO newInstance(DaRemoteRepository extSystem) {
        return new DaRemoteRepositorySimpleVO(extSystem);
    }

}
