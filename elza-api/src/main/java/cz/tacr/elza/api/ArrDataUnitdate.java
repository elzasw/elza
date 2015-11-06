package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu strojově zpracovatelná datace.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataUnitdate<CT extends ArrCalendarType> extends Serializable {

    String getValueFrom();

    void setValueFrom(String valueFrom);

    Boolean getValueFromEstimated();

    void setValueFromEstimated(Boolean valueFromEstimated);

    String getValueTo();

    void setValueTo(String valueTo);

    Boolean getValueToEstimated();

    void setValueToEstimated(Boolean valueToEstimated);

    Integer getCalendarTypeId();

    void setCalendarTypeId(Integer calendarTypeId);

    String getFormat();

    void setFormat(String format);

}
