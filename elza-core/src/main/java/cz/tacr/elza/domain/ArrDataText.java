package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu "neomezený" textový řetězec.
 */
@Entity(name = "arr_data_text")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataText extends ArrData {

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String value;

	public ArrDataText() {

	}

	protected ArrDataText(ArrDataText src) {
    	super(src);
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
	public ArrDataText makeCopy() {
		return new ArrDataText(this);
	}
}
