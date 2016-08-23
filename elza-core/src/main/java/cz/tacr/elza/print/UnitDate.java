package cz.tacr.elza.print;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.api.ArrCalendarType;
import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * Rozšiřuje {@link UnitDateText} o strukturovaný zápis datumu.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class UnitDate extends UnitDateText implements IUnitdate {

    private String valueFrom;
    private String valueTo;
    private Boolean valueFromEstimated;
    private Boolean valueToEstimated;
    private String format;
    private String calendar;
    private String calendarCode;
    private ArrCalendarType calendarType;

    /**
     * @return hodnota valueText
     */
    @Override
    public String serialize() {
        if (StringUtils.isNotBlank(getValueText())) {
            return super.serialize();
        }
        return UnitDateConvertor.convertToString(this);
    }

    public String getCalendar() {
        return calendar;
    }

    public void setCalendar(final String calendar) {
        this.calendar = calendar;
    }

    public String getCalendarCode() {
        return calendarCode;
    }

    public void setCalendarCode(final String calendarCode) {
        this.calendarCode = calendarCode;
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
    public String getValueFrom() {
        return valueFrom;
    }

    @Override
    public void setValueFrom(final String valueFrom) {
        this.valueFrom = valueFrom;
    }

    @Override
    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    @Override
    public void setValueFromEstimated(final Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    @Override
    public String getValueTo() {
        return valueTo;
    }

    @Override
    public void setValueTo(final String valueTo) {
        this.valueTo = valueTo;
    }

    @Override
    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    @Override
    public void setValueToEstimated(final Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    @Override
    public ArrCalendarType getCalendarType() {
        return calendarType;
    }

    @Override
    public void setCalendarType(final ArrCalendarType calendarType) {
        this.calendarType = calendarType;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
