package cz.tacr.elza.domain;

import java.util.Objects;

import cz.tacr.elza.domain.interfaces.IArrItemStringValue;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemFormattedText extends ArrItemData implements IArrItemStringValue {

    private String value;

    @Override
	public String getValue() {
        return value;
    }

    @Override
	public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemFormattedText that = (ArrItemFormattedText) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
