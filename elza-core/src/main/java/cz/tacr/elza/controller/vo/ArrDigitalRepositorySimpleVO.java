package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDigitalRepository;

/**
 * VO pro externí systém - uložiště digitalizátů.
 *
 * @since 05.12.2016
 */
public class ArrDigitalRepositorySimpleVO extends SysExternalSystemSimpleVO {

    private ArrDigitalRepositorySimpleVO(ArrDigitalRepository extSystem) {
        super(extSystem);
    }

    public static SysExternalSystemSimpleVO newInstance(ArrDigitalRepository extSystem) {
        return new ArrDigitalRepositorySimpleVO(extSystem);
    }

}
