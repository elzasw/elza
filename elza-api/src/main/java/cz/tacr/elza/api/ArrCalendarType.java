package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * Typ kalendáře.
 *
 * @author Martin Šlapa
 * @since 20.10.2015
 */
public interface ArrCalendarType extends Serializable {

    Integer getCalendarTypeId();

    void setCalendarTypeId(Integer calendarTypeId);

    String getCode();

    void setCode(final String code);

    String getName();

    void setName(final String name);

}
