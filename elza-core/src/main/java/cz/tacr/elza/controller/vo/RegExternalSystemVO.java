package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.RegExternalSystemType;

/**
 * VO pro externí systém.
 *
 * @author Martin Šlapa
 * @since 05.12.2016
 */
public class RegExternalSystemVO extends SysExternalSystemVO {

    private RegExternalSystemType type;

    public RegExternalSystemType getType() {
        return type;
    }

    public void setType(final RegExternalSystemType type) {
        this.type = type;
    }
}
