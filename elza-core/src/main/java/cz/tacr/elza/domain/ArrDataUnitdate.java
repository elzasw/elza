package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_unitdate")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitdate extends ArrData implements cz.tacr.elza.api.ArrDataUnitdate<ArrCalendarType> {

    @Column(nullable = false)
    private Integer calendarTypeId;

    @Column(length = 19, nullable = true)
    private String valueFrom;

    @Column(nullable = false)
    private Boolean valueFromEstimated;

    @Column(length = 19, nullable = true)
    private String valueTo;

    @Column(nullable = false)
    private Boolean valueToEstimated;


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
    public Integer getCalendarTypeId() {
        return this.calendarTypeId;
    }

    @Override
    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }


}
