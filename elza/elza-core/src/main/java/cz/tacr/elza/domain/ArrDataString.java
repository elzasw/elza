package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    public static final String STRING_VALUE = "stringValue";

    @Column(name = "value", length = StringLength.LENGTH_1000, nullable = false)
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

    @Override
    protected void validateInternal() {
        Validate.notNull(stringValue);
        // check any leading and trailing whitespace in data
        String value = stringValue.trim();
        Validate.isTrue(value.length() == stringValue.length(), "Value obsahuje whitespaces na začátku nebo na konci, dataId: ", getDataId());
        // check for non-printable chars in the string, exclude 0x0D, 0x0A
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            Validate.isTrue(c >= 0x1f, "Value obsahuje netiskové znaky, dataId: ", getDataId());
        }
        // check double-space
        Validate.isTrue(value.indexOf("  ") < 0, "Value obsahuje dvojité mezery, dataId: ", getDataId());
    }
}
