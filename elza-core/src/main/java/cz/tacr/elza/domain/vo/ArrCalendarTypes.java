package cz.tacr.elza.domain.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.domain.ArrCalendarType;


/**
 * Zapouzdření seznamu calendářů.
 *
 * @author Martin Šlapa
 * @since 20.10.2015
 */
public class ArrCalendarTypes {

    private List<ArrCalendarType> calendarTypes;

    public List<ArrCalendarType> getCalendarTypes() {
        return this.calendarTypes;
    }

    public void setCalendarTypes(final List<ArrCalendarType> calendarTypes) {
        this.calendarTypes = calendarTypes;
    }

}
