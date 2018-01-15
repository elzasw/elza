package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.RegExternalSystemType;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 */
public class RegExternalSystemVO extends SysExternalSystemVO {

    private RegExternalSystemType type;

    public RegExternalSystemType getType() {
        return type;
    }

    public void setType(final RegExternalSystemType type) {
        this.type = type;
    }

    @Override
    public SysExternalSystem createEntity() {
        RegExternalSystem entity = new RegExternalSystem();
        entity.setType(type);
        this.fillEntity(entity);
        return entity;
    }
}
