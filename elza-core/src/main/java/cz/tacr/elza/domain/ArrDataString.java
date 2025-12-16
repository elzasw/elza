package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.validation.ValidStringField;

/**
 * Hodnota atributu archivního popisu typu omezený textový řetězec.
 */
@Entity(name = "arr_data_string")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataString extends ArrData {

    public static final String STRING_VALUE = "stringValue";

    @ValidStringField
    @Column(name = "string_value", length = StringLength.LENGTH_1000, nullable = false)
    private String stringValue;

	public ArrDataString() {

	}

    public ArrDataString(final String stringValue) {
        setStringValue(stringValue);
    }

	protected ArrDataString(ArrDataString src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataString src) {
        this.stringValue = src.stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(final String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String getFulltextValue() {
        return stringValue;
    }

	@Override
	public ArrDataString makeCopy() {
		return new ArrDataString(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataString src = (ArrDataString)srcData;
        return stringValue.equals(src.stringValue);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataString src = (ArrDataString) srcData;
        copyValue(src);
    }

}
