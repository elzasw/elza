package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;

/**
 * VO hodnoty atributu - unit date.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemUnitdateVO extends ArrItemVO {

    /**
     * hodnota
     */
    private String value;

    /**
     * identifikátor kalendáře
     */
    private Integer calendarTypeId;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        // prepare calendar type
        CalendarType calType = CalendarType.fromId(calendarTypeId);
        Validate.notNull(calType);

        ArrDataUnitdate data = ArrDataUnitdate.valueOf(calType, value);
        return data;
    }
}