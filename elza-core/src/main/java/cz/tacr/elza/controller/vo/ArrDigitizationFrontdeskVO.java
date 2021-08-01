package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém - seznam digitalizačních linek.
 *
 */
public class ArrDigitizationFrontdeskVO extends SysExternalSystemVO {

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        ArrDigitizationFrontdesk entity = new ArrDigitizationFrontdesk();
        this.fillEntity(entity);

        return entity;
    }

}
