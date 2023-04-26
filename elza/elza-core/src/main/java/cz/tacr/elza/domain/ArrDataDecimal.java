package cz.tacr.elza.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hodnota atributu archivního popisu typu desetinného čísla.
 */
@Entity(name = "arr_data_decimal")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataDecimal extends ArrData {

    @Column(name = "decimal_value", nullable = false)
    private BigDecimal value;

	public ArrDataDecimal() {
	}

	protected ArrDataDecimal(final ArrDataDecimal src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataDecimal src) {
        this.value = src.value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value.toPlainString();
    }

    @Override
    public Double getValueDouble() {
        return value.doubleValue();
    }

	@Override
	public ArrDataDecimal makeCopy() {
		return new ArrDataDecimal(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataDecimal src = (ArrDataDecimal)srcData;
        return value.equals(src.value);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataDecimal src = (ArrDataDecimal)srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(value);
    }
}
