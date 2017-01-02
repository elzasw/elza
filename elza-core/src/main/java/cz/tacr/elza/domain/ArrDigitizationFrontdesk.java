package cz.tacr.elza.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * Digitalizační linka.
 *
 * @author Martin Šlapa
 * @since 05. 12. 2016
 */
@Entity(name = "arr_digitization_frontdesk")
@Table
public class ArrDigitizationFrontdesk extends SysExternalSystem implements cz.tacr.elza.api.ArrDigitizationFrontdesk {

    @Override
    public String toString() {
        return "ArrDigitizationFrontdesk pk=" + getExternalSystemId();
    }
}
