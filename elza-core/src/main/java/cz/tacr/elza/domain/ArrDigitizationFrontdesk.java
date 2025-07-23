package cz.tacr.elza.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Digitalizační linka.
 *
 * @since 05. 12. 2016
 */
@Entity(name = "arr_digitization_frontdesk")
@Table
public class ArrDigitizationFrontdesk extends SysExternalSystem {
	
	public ArrDigitizationFrontdesk() {
		
	}

    public ArrDigitizationFrontdesk(ArrDigitizationFrontdesk adf) {
		super(adf);
	}

	@Override
    public String toString() {
        return "ArrDigitizationFrontdesk pk=" + getExternalSystemId();
    }
}
