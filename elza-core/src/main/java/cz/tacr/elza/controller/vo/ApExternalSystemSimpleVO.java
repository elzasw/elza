package cz.tacr.elza.controller.vo;


import cz.tacr.elza.api.ApExternalSystem;

/**
 * VO pro externí systém.
 *
 * @author Martin Šlapa
 * @since 05.12.2016
 */
public class ApExternalSystemSimpleVO extends SysExternalSystemSimpleVO {

    private ApExternalSystem type;

    public ApExternalSystem getType() {
        return type;
    }

    public void setType(final ApExternalSystem type) {
        this.type = type;
    }

}
