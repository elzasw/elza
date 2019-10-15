package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Hodnota atributu archivního popisu typu omezený textový řetězec.
 */
@Entity(name = "arr_data_string")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataString extends ArrData {

    @Column(length = StringLength.LENGTH_1000, nullable = false)
    private String value;

	public ArrDataString() {

	}

    public ArrDataString(final String value) {
        setValue(value);
    }

	protected ArrDataString(ArrDataString src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataString src) {
        this.value = src.value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value;
    }

	@Override
	public ArrDataString makeCopy() {
		return new ArrDataString(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataString src = (ArrDataString)srcData;
        return value.equals(src.value);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataString src = (ArrDataString) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(value);
    }
}
