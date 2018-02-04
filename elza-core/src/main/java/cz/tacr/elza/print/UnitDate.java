package cz.tacr.elza.print;

import java.util.Objects;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * Rozšiřuje {@link UnitDateText} o strukturovaný zápis datumu.
 *
 */
public class UnitDate implements IUnitdate {

    private final String valueFrom;

    private final String valueTo;

    private final Boolean valueFromEstimated;

    private final Boolean valueToEstimated;

    private final CalendarType calendarType;

    private String format;

    private String valueText;

    public UnitDate(IUnitdate srcItemData) {
        this.valueFrom = srcItemData.getValueFrom();
        this.valueTo = srcItemData.getValueTo();
        this.valueFromEstimated = srcItemData.getValueFromEstimated();
        this.valueToEstimated = srcItemData.getValueToEstimated();
        this.format = srcItemData.getFormat();
        // id without fetch -> access type property
        this.calendarType = CalendarType.fromId(srcItemData.getCalendarType().getCalendarTypeId());
    }

    public String getValueText() {
        if (valueText == null) {
            valueText = UnitDateConvertor.convertToString(this);
        }
        return valueText;
    }

    public String getCalendar() {
        return calendarType.getName();
    }

    public String getCalendarCode() {
        return calendarType.getCode();
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(final String format) {
        if (!Objects.equals(this.format, format)) {
            resetValueText();
        }
        this.format = format;
    }

    @Override
    public void formatAppend(final String format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValueFrom() {
        return valueFrom;
    }

    @Override
    public void setValueFrom(final String valueFrom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    @Override
    public void setValueFromEstimated(final Boolean valueFromEstimated) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValueTo() {
        return valueTo;
    }

    @Override
    public void setValueTo(final String valueTo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    @Override
    public void setValueToEstimated(final Boolean valueToEstimated) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrCalendarType getCalendarType() {
        return calendarType.getEntity();
    }

    @Override
    public void setCalendarType(ArrCalendarType calendarType) {
        throw new UnsupportedOperationException();
    }

    private void resetValueText() {
        this.valueText = null;
    }
}
