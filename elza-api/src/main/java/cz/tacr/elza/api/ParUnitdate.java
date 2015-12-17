package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Hodnoty datace.
 *
 * @param  <CT> typ kalendáře
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParUnitdate<CT extends ArrCalendarType> extends Versionable, Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getUnitdateId();

    void setUnitdateId(Integer unitdateId);

    CT getCalendarType();

    void setCalendarType(CT calendarType);

    String getValueFrom();

    void setValueFrom(String valueFrom);

    Boolean getValueFromEstimated();

    void setValueFromEstimated(Boolean valueFromEstimated);

    String getValueTo();

    void setValueTo(String valueTo);

    Boolean getValueToEstimated();

    void setValueToEstimated(Boolean valueToEstimated);

    String getFormat();

    void setFormat(String format);

    String getTextDate();

    void setTextDate(String textDate);
}
