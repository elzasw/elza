package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemInt extends ArrItemData {

    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value == null ? null : value.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemInt itemInt = (ArrItemInt) o;
        return Objects.equals(value, itemInt.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
