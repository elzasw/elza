package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Hodnota atributu archivního popisu typu referenční označení.
 */
@Entity(name = "arr_data_unitid")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitid extends ArrData {

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String value;

	public ArrDataUnitid() {

	}

	protected ArrDataUnitid(ArrDataUnitid src) {
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
	public ArrDataUnitid makeCopy() {
		return new ArrDataUnitid(this);
	}
}
