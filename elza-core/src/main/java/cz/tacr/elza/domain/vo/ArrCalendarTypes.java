package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrCalendarType;


/**
 * Zapouzdření seznamu calendářů.
 *
 * @author Martin Šlapa
 * @since 20.10.2015
 */
public class ArrCalendarTypes implements cz.tacr.elza.api.vo.ArrCalendarTypes<ArrCalendarType> {

    private List<ArrCalendarType> calendarTypes;

    @Override
    public List<ArrCalendarType> getCalendarTypes() {
        return this.calendarTypes;
    }

    @Override
    public void setCalendarTypes(final List<ArrCalendarType> calendarTypes) {
        this.calendarTypes = calendarTypes;
    }
    
}
