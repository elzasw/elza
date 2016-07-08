package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.api.ArrCalendarType;
import cz.tacr.elza.api.ArrDescItem;


/**
 * Zapouzdření seznamu kalendářů {@link ArrCalendarType}.
 *
 * @param <CT> {@link ArrDescItem}
 * @author Martin Šlapa
 * @since 20.10.2015
 */
public interface ArrCalendarTypes<CT extends ArrCalendarType> extends Serializable {

    List<CT> getCalendarTypes();

    void setCalendarTypes(List<CT> calendarTypes);

}
