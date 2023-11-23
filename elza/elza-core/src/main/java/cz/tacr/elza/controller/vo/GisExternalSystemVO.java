package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.GisSystemType;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí Gis systém.
 */
public class GisExternalSystemVO extends SysExternalSystemVO {

    private GisSystemType type;

    public GisSystemType getType() {
        return type;
    }

    public void setType(GisSystemType type) {
        this.type = type;
    }

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        return null;
    }
}
