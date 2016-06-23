package cz.tacr.elza.print;

import cz.tacr.elza.api.ArrCalendarType;
import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

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
    public String serialize() {
        if (StringUtils.isNotBlank(getValueText())) {
            return super.serialize();
        } else {
            return UnitDateConvertor.convertToString(this);
        }
    }

    public String getCalendar() {
        return calendar;
    }

    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    public String getCalendarCode() {
        return calendarCode;
    }

    public void setCalendarCode(String calendarCode) {
        this.calendarCode = calendarCode;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public void formatAppend(String format) {
        this.format += format;
    }

    public String getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(String valueFrom) {
        this.valueFrom = valueFrom;
    }

    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    public void setValueFromEstimated(Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    public String getValueTo() {
        return valueTo;
    }

    public void setValueTo(String valueTo) {
        this.valueTo = valueTo;
    }

    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    public void setValueToEstimated(Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    @Override
    public ArrCalendarType getCalendarType() {
        return calendarType;
    }

    @Override
    public void setCalendarType(ArrCalendarType calendarType) {
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
