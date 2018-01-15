package cz.tacr.elza.controller.vo.nodes.descitems;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.persistence.EntityManager;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

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
        ArrDataUnitdate data = new ArrDataUnitdate();

        // prepare calendar type
        CalendarType calType = CalendarType.fromId(calendarTypeId);
        Validate.notNull(calType);
        data.setCalendarType(calType.getEntity());

        UnitDateConvertor.convertToUnitDate(value, data);

        // prepare normalized values - from
        String valueFrom = data.getValueFrom();
        Long normalizedFrom;
        if (valueFrom != null) {
            LocalDateTime locDateTime = LocalDateTime.parse(valueFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            normalizedFrom = CalendarConverter.toSeconds(calType, locDateTime);
        } else {
            normalizedFrom = Long.MIN_VALUE;
        }
        data.setNormalizedFrom(normalizedFrom);

        // prepare normalized values - to
        String valueTo = data.getValueTo();
        Long normalizedTo;
        if (valueTo != null) {
            LocalDateTime locDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            normalizedTo = CalendarConverter.toSeconds(calType, locDateTime);
        } else {
            normalizedTo = Long.MAX_VALUE;
        }
        data.setNormalizedTo(normalizedTo);

        data.setDataType(DataType.UNITDATE.getEntity());
        return data;
    }
}