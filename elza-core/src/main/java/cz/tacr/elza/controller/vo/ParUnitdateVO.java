package cz.tacr.elza.controller.vo;

/**
 * Hodnota datace.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParUnitdateVO {

    private Integer unitdateId;

    private ArrCalendarTypeVO calendarType;

    private String valueFrom;

    private Boolean valueFromEstimated;

    private String valueTo;

    private Boolean valueToEstimated;

    private String format;

    private String textDate;

    public Integer getUnitdateId() {
        return unitdateId;
    }

    public void setUnitdateId(final Integer unitdateId) {
        this.unitdateId = unitdateId;
    }

    public ArrCalendarTypeVO getCalendarType() {
        return calendarType;
    }

    public void setCalendarType(final ArrCalendarTypeVO calendarType) {
        this.calendarType = calendarType;
    }

    public String getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(final String valueFrom) {
        this.valueFrom = valueFrom;
    }

    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    public void setValueFromEstimated(final Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    public String getValueTo() {
        return valueTo;
    }

    public void setValueTo(final String valueTo) {
        this.valueTo = valueTo;
    }

    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    public void setValueToEstimated(final Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getTextDate() {
        return textDate;
    }

    public void setTextDate(final String textDate) {
        this.textDate = textDate;
    }
}
