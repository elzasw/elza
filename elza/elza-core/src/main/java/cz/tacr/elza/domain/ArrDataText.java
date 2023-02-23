package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu "neomezený" textový řetězec.
 */
@Entity(name = "arr_data_text")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataText extends ArrData {

    public static final String TEXT_VALUE = "textValue";

    @Column(name = "value", nullable = false)
    @Lob
    //@Type(type = "org.hibernate.type.TextType") TODO pasek
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
        Validate.notNull(textValue);
        // check any leading and trailing whitespace in data
        String value = textValue.trim();
        Validate.isTrue(value.length() == textValue.length(), "Value obsahuje whitespaces na začátku nebo na konci, dataId: ", getDataId());
        // check for non-printable chars in the string, exclude 0x0D, 0x0A
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            Validate.isTrue(c >= 0x1f || c == 0x0D || c == 0x0A, "Value obsahuje netiskové znaky, dataId: ", getDataId());
        }
    }
}
