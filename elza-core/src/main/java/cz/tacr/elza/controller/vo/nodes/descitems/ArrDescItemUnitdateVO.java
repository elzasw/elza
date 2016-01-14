package cz.tacr.elza.controller.vo.nodes.descitems;

/**
 * VO hodnoty atributu - unit date.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemUnitdateVO extends ArrDescItemVO {

    /**
     * od data
     */
    private String valueFrom;

    /**
     * přibližně od
     */
    private Boolean valueFromEstimated;

    /**
     * do data
     */
    private String valueTo;

    /**
     * přibližně do
     */
    private Boolean valueToEstimated;

    /**
     * identifikátor kalendáře
     */
    private Integer calendarTypeId;

    /**
     * formát uložených dat
     */
    private String format;

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

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }
}