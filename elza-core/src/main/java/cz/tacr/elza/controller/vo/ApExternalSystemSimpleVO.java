package cz.tacr.elza.controller.vo;


import cz.tacr.elza.api.ApExternalSystemType;

/**
 * VO pro externí systém.
 *
 * @author Martin Šlapa
 * @since 05.12.2016
 */
public class ApExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    private ApExternalSystemType type;

    public ApExternalSystemType getType() {
        return type;
    }

    public void setType(final ApExternalSystemType type) {
        this.type = type;
    }

}
