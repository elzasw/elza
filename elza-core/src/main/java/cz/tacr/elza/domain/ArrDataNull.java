package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Atributu archivního popisu bez hodnoty - napr. enum
 */
@Entity(name = "arr_data_null")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataNull extends ArrData {

	public ArrDataNull() {

	}

	protected ArrDataNull(ArrDataNull src) {
		super(src);
	}

	@Override
    public String getFulltextValue() {
        return null;
    }

	@Override
	public ArrDataNull makeCopy() {
		return new ArrDataNull(this);
	}

}
