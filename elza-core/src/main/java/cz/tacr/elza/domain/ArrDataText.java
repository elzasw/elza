package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.Length;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

}
