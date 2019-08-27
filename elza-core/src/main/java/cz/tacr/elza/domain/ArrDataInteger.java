package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu Integer.
 */
@Entity(name = "arr_data_integer")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataInteger extends ArrData {

    @Column(nullable = false)
    private Integer value;

	public ArrDataInteger() {

	}

    public ArrDataInteger(final Integer value) {
        setValue(value);
    }

	protected ArrDataInteger(ArrDataInteger src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataInteger src) {
        this.value = src.value;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value.toString();
    }

    @Override
    public Integer getValueInt() {
        return value;
    }

	@Override
	public ArrDataInteger makeCopy() {
		return new ArrDataInteger(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataInteger src = (ArrDataInteger)srcData;
        return value.equals(src.value);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataInteger src = (ArrDataInteger) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(value);
    }
}
