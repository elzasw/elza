package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_unitdate")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitdate extends ArrData implements cz.tacr.elza.api.ArrDataUnitdate<ArrCalendarType> {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrCalendarType.class)
    @JoinColumn(name = "calendarTypeId", nullable = false)
    private ArrCalendarType calendarType;

    @Column(length = 19, nullable = true)
    private String valueFrom;

    @Column(nullable = false)
    private Boolean valueFromEstimated;

    @Column(length = 19, nullable = true)
    private String valueTo;

    @Column(nullable = false)
    private Boolean valueToEstimated;

    @Column(length = 50, nullable = false)
    private String format;

    @Column(nullable = false)
    private Long normalizedFrom;

    @Column(nullable = false)
    private Long normalizedTo;

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
        return calendarType;
    }

    @Override
    public void setCalendarType(ArrCalendarType calendarType) {
        this.calendarType = calendarType;
    }


    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public Long getNormalizedFrom() {
        return normalizedFrom;
    }

    @Override
    public void setNormalizedFrom(final Long normalizedFrom) {
        this.normalizedFrom = normalizedFrom;
    }

    @Override
    public Long getNormalizedTo() {
        return normalizedTo;
    }

    @Override
    public void setNormalizedTo(final Long normalizedTo) {
        this.normalizedTo = normalizedTo;
    }

    @Override
    public String getFulltextValue() {
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

        String unitdateString = UnitDateConvertor.convertToString(this);

        return ret + " " + unitdateString;
    }

    @Override
    public void formatAppend(final String format) {
        this.format += format;
    }
}
