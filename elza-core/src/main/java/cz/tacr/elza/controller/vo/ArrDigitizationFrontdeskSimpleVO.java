package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDigitizationFrontdesk;

/**
 * VO pro externí systém - seznam digitalizačních linek.
 *
 * @since 05.12.2016
 */
public class ArrDigitizationFrontdeskSimpleVO extends SysExternalSystemSimpleVO {

    private ArrDigitizationFrontdeskSimpleVO(ArrDigitizationFrontdesk extSystem) {
        super(extSystem);
    }

    public static ArrDigitizationFrontdeskSimpleVO newInstance(ArrDigitizationFrontdesk extSystem) {
        return new ArrDigitizationFrontdeskSimpleVO(extSystem);
    }

}
