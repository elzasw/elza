package cz.tacr.elza.domain;


/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemUnitdate extends ArrItemData implements cz.tacr.elza.api.ArrItemUnitdate<ArrCalendarType> {

    private String valueFrom;

    private Boolean valueFromEstimated;

    private String valueTo;

    private Boolean valueToEstimated;

    private ArrCalendarType calendarType;

    private String format;

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
}
