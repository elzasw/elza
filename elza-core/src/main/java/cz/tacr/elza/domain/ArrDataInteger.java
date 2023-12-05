package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu Integer.
 */
@Entity(name = "arr_data_integer")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataInteger extends ArrData {

    public static final String INTEGER_VALUE = "integerValue";

    @Column(name = "integerValue", nullable = false)
    private Integer integerValue;

	public ArrDataInteger() {

	}

    public ArrDataInteger(final Integer integerValue) {
        setIntegerValue(integerValue);
    }

	protected ArrDataInteger(ArrDataInteger src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataInteger src) {
        this.integerValue = src.integerValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(final Integer integerValue) {
        this.integerValue = integerValue;
    }

    @Override
    public String getFulltextValue() {
        return integerValue.toString();
    }

    @Override
    public Integer getValueInt() {
        return integerValue;
    }

	@Override
	public ArrDataInteger makeCopy() {
		return new ArrDataInteger(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataInteger src = (ArrDataInteger)srcData;
        return integerValue.equals(src.integerValue);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataInteger src = (ArrDataInteger) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(integerValue);
    }
}
