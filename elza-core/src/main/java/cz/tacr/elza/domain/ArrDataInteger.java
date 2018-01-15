package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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

	protected ArrDataInteger(ArrDataInteger src) {
		super(src);
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
}
