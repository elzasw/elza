package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

import org.hibernate.Length;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.validation.ValidStringField;

/**
 * Hodnota atributu archivního popisu typu "neomezený" textový řetězec.
 */
@Entity(name = "arr_data_text")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataText extends ArrData {

    public static final String TEXT_VALUE = "textValue";

    @ValidStringField(multiline = true)
    @Column(length = Length.LONG, nullable = false) // Hibernate long text field
    private String textValue;

	public ArrDataText() {

	}

    public ArrDataText(final String textValue) {
        setTextValue(textValue);
    }

	protected ArrDataText(ArrDataText src) {
    	super(src);
        copyValue(src);
    }

    private void copyValue(ArrDataText src) {
        this.textValue = src.textValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(final String textValue) {
        this.textValue = textValue;
    }

    @Override
    public String getFulltextValue() {
        return textValue;
    }

	@Override
	public ArrDataText makeCopy() {
		return new ArrDataText(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataText src = (ArrDataText)srcData;
        return textValue.equals(src.textValue);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataText src = (ArrDataText) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
    	Objects.requireNonNull(textValue);
        // check any leading and trailing whitespace in data
        String value = textValue.trim();
        if (value.length() != textValue.length()) {
            throw new BusinessException("Value contains whitespaces at the begining/end",
                    BaseCode.PROPERTY_IS_INVALID)
                            .set("dataId", getDataId())
                            .set("property", textValue);
        }
        // check for non-printable chars in the string, exclude 0x0D, 0x0A
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1f && c != 0x0D && c != 0x0A) {
                throw new BusinessException("Value contains invalid characters.",
                        BaseCode.PROPERTY_IS_INVALID)
                                .set("dataId", getDataId())
                                .set("property", textValue)
                                .set("invalidCharacter", Integer.valueOf(c));
            }
        }
    }
}
