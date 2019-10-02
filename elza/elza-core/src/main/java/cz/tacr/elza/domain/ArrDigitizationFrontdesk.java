package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Digitalizační linka.
 *
 * @author Martin Šlapa
 * @since 05. 12. 2016
 */
@Entity(name = "arr_digitization_frontdesk")
@Table
public class ArrDigitizationFrontdesk extends SysExternalSystem {

    @Override
    public String toString() {
        return "ArrDigitizationFrontdesk pk=" + getExternalSystemId();
    }
}
