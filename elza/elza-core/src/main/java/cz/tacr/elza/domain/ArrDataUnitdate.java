package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.common.db.CharConverter;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.convertor.CalendarConverter;
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

    /* Příklad:
     * -0015-01-01T00:00:00 - datum může mít znaménko minus
     */
    @Column(length = 20, nullable = true)
    @Convert(converter = CharConverter.class)
    private String valueFrom;

    @Column(nullable = false)
    private Boolean valueFromEstimated;

    /* Příklad:
     * -0015-01-01T00:00:00 - datum může mít znaménko minus
     */
    @Column(length = 20, nullable = true)
    @Convert(converter = CharConverter.class)
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
	    
	    if(!Objects.equal(valueFrom, src.valueFrom)||
	       !Objects.equal(valueFromEstimated, src.valueFromEstimated)||
	       !Objects.equal(valueTo, src.valueTo)||
	       !Objects.equal(valueToEstimated, src.valueToEstimated)||
	       !Objects.equal(format, src.format)) {
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
        Validate.notNull(format);
        Validate.notNull(normalizedFrom);
        Validate.notNull(normalizedTo);
        Validate.notNull(valueFromEstimated);
        Validate.notNull(valueToEstimated);
    }

    static public ArrDataUnitdate valueOf(String v) {
        ArrDataUnitdate du = new ArrDataUnitdate();

        UnitDateConvertor.convertToUnitDate(v, du);

        // set normalized values
        String valueFrom = du.getValueFrom();
        if (valueFrom == null) {
            du.setNormalizedFrom(Long.MIN_VALUE);
        } else {
            LocalDateTime fromDate = LocalDateTime.parse(valueFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            du.setNormalizedFrom(CalendarConverter.toSeconds(fromDate));
        }

        String valueTo = du.getValueTo();
        if (valueTo == null) {
            du.setNormalizedTo(Long.MAX_VALUE);
        } else {
            LocalDateTime toDate = LocalDateTime.parse(valueTo, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            du.setNormalizedTo(CalendarConverter.toSeconds(toDate));
        }

        du.setDataType(DataType.UNITDATE.getEntity());

        return du;
    }
}
