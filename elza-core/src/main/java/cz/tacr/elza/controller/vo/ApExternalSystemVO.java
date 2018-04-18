package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ApExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 */
public class ApExternalSystemVO extends SysExternalSystemVO {

    private ApExternalSystem type;

    public ApExternalSystem getType() {
        return type;
    }

    public void setType(final ApExternalSystem type) {
        this.type = type;
    }

    @Override
    public SysExternalSystem createEntity() {
        cz.tacr.elza.domain.ApExternalSystem entity = new cz.tacr.elza.domain.ApExternalSystem();
        entity.setType(type);
        this.fillEntity(entity);
        return entity;
    }
}
