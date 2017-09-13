package cz.tacr.elza.domain;


import java.util.Objects;

import cz.tacr.elza.api.IUnitdate;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemUnitdate extends ArrItemData implements IUnitdate {

    private String valueFrom;

    private Boolean valueFromEstimated;

    private String valueTo;

    private Boolean valueToEstimated;

    private ArrCalendarType calendarType;

    private Integer calendarTypeId;

    private String format;

    private Long normalizedTo;

    private Long normalizedFrom;

    @Override
    public String getValueFrom() {
        return this.valueFrom;
    }

    @Override
    public void setValueFrom(final String valueFrom) {
        this.valueFrom = valueFrom;
    }

    @Override
    public Boolean getValueFromEstimated() {
        return this.valueFromEstimated;
    }

    @Override
    public void setValueFromEstimated(final Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    @Override
    public String getValueTo() {
        return this.valueTo;
    }

    @Override
    public void setValueTo(final String valueTo) {
        this.valueTo = valueTo;
    }

    @Override
    public Boolean getValueToEstimated() {
        return this.valueToEstimated;
    }

    @Override
    public void setValueToEstimated(final Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    @Override
    public ArrCalendarType getCalendarType() {
        return this.calendarType;
    }

    @Override
    public void setCalendarType(final ArrCalendarType calendarType) {
        this.calendarType = calendarType;
        this.calendarTypeId = calendarType == null ? null : calendarType.getCalendarTypeId();
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(final String format) {
        this.format = format;
    }

    @Override
    public void formatAppend(final String format) {
        this.format += format;
    }

    @Override
    public String toString() {

        String ret = calendarType == null ? "?" : calendarType.getName() + " ";

        String from = valueFromEstimated == true ? valueFrom + "*" : valueFrom;
        String to = valueToEstimated == true ? valueTo + "*" : valueTo;

        if (valueFrom != null && valueTo != null) {
            ret += from + " - " + to;
        } else if (valueTo != null) {
            ret += " do " + to;
        } else if (valueFrom != null) {
            ret += " od " + from;
        } else {
            ret += " ?";
        }

        return ret;

    }

    public void setNormalizedTo(final Long normalizedTo) {
        this.normalizedTo = normalizedTo;
    }

    public Long getNormalizedTo() {
        return normalizedTo;
    }

    public void setNormalizedFrom(final Long normalizedFrom) {
        this.normalizedFrom = normalizedFrom;
    }

    public Long getNormalizedFrom() {
        return normalizedFrom;
    }

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemUnitdate that = (ArrItemUnitdate) o;
        return Objects.equals(valueFrom, that.valueFrom) &&
                Objects.equals(valueFromEstimated, that.valueFromEstimated) &&
                Objects.equals(valueTo, that.valueTo) &&
                Objects.equals(valueToEstimated, that.valueToEstimated) &&
                Objects.equals(calendarType, that.calendarType) &&
                Objects.equals(format, that.format) &&
                Objects.equals(normalizedTo, that.normalizedTo) &&
                Objects.equals(normalizedFrom, that.normalizedFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), valueFrom, valueFromEstimated, valueTo, valueToEstimated, calendarType, format, normalizedTo, normalizedFrom);
    }
}
