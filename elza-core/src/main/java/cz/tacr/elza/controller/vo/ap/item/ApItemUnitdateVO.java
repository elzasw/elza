package cz.tacr.elza.controller.vo.ap.item;

import javax.persistence.EntityManager;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * @since 18.07.2018
 */
public class ApItemUnitdateVO extends ApItemVO {

    /**
     * hodnota
     */
    private String value;

    /**
     * identifikátor kalendáře
     */
    private Integer calendarTypeId;

    public ApItemUnitdateVO() {
    }

    public ApItemUnitdateVO(final ApItem item) {
        super(item);
        ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
        if (data != null) {
            value = UnitDateConvertor.convertToString(data);
            calendarTypeId = data.getCalendarTypeId();
        }
    }

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
