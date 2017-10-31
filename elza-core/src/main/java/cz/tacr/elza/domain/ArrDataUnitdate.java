package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;


/**
 * Hodnota atributu archivního popisu typu strojově zpracovatelná datace.
 */
@Entity(name = "arr_data_unitdate")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitdate extends ArrData implements IUnitdate {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrCalendarType.class)
    @JoinColumn(name = "calendarTypeId", nullable = false)
    private ArrCalendarType calendarType;

    @Column(name = "calendarTypeId", updatable = false, insertable = false)
    private Integer calendarTypeId;

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

    public Integer getCalendarTypeId() {
        return calendarTypeId;
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

    /**
     * @return počet sekund v normalizačním kalendáři - od
     */
    @Override
    public Long getNormalizedFrom() {
        return normalizedFrom;
    }

    /**
     * @param normalizedFrom počet sekund v normalizačním kalendáři - od
     */
    public void setNormalizedFrom(final Long normalizedFrom) {
        this.normalizedFrom = normalizedFrom;
    }

    /**
     * @return počet sekund v normalizačním kalendáři - do
     */
    @Override
    public Long getNormalizedTo() {
        return normalizedTo;
    }

    /**
     * @param normalizedTo počet sekund v normalizačním kalendáři - do
     */
    public void setNormalizedTo(final Long normalizedTo) {
        this.normalizedTo = normalizedTo;
    }

    @Override
    public String getFulltextValue() {
        String unitdateString = UnitDateConvertor.convertToString(this);
        return unitdateString;
    }

    @Override
    public void formatAppend(final String format) {
        this.format += format;
    }
}
