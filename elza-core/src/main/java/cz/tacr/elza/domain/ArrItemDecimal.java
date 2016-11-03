package cz.tacr.elza.domain;

import java.math.BigDecimal;
import java.util.Objects;


/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 12.10.2015
 */
public class ArrItemDecimal extends ArrItemData implements cz.tacr.elza.api.ArrItemDecimal {

    private BigDecimal value;

    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {return value == null ? null : value.toPlainString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemDecimal that = (ArrItemDecimal) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
