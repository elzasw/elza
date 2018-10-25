package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang3.Validate;

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

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        return true;
    }
    @Override
    public void mergeInternal(final ArrData srcData) {
        Validate.isInstanceOf(ArrDataNull.class, srcData);
    }

    @Override
    protected void validateInternal() {
        // nothing to check        
    }
}
