package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;


/**
 * Hodnota atributu archivního popisu typu strojově zpracovatelná datace.
 */
@Entity(name = "arr_data_unitdate")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitdate extends ArrData implements IUnitdate {

    public static final String NORMALIZED_FROM = "normalizedFrom";
    public static final String NORMALIZED_TO = "normalizedTo";

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

	public ArrDataUnitdate() {

	}

	protected ArrDataUnitdate(ArrDataUnitdate src) {
		super(src);

        copyValue(src);
    }

    private void copyValue(ArrDataUnitdate src) {
        this.calendarType = src.calendarType;
        this.calendarTypeId = src.calendarTypeId;
        this.format = src.format;
        this.normalizedFrom = src.normalizedFrom;
        this.normalizedTo = src.normalizedTo;
        this.valueFrom = src.valueFrom;
        this.valueFromEstimated = src.valueFromEstimated;
        this.valueTo = src.valueTo;
        this.valueToEstimated = src.valueToEstimated;
    }

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
    @JsonIgnore(false)
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
    @JsonIgnore(false)
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

	@Override
	public ArrDataUnitdate makeCopy() {
		return new ArrDataUnitdate(this);
	}

	@Override
    protected boolean isEqualValueInternal(ArrData srcData) {
	    ArrDataUnitdate src = (ArrDataUnitdate)srcData;
	    
	    if(!calendarTypeId.equals(src.calendarTypeId)||
	       !Objects.equal(valueFrom, src.valueFrom)||
	       !Objects.equal(valueFromEstimated, src.valueFromEstimated)||
	       !Objects.equal(valueTo, src.valueTo)||
	       !Objects.equal(valueToEstimated, src.valueToEstimated)||
	       !Objects.equal(format, src.format)
	            )
	    {
	        return false;
	    }
        return true;
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataUnitdate src = (ArrDataUnitdate) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(calendarType);
        Validate.notNull(calendarTypeId);
        Validate.notNull(format);
        Validate.notNull(normalizedFrom);
        Validate.notNull(normalizedTo);
        Validate.notNull(valueFromEstimated);
        Validate.notNull(valueToEstimated);
    }
}
