package cz.tacr.elza.domain;

import cz.tacr.elza.domain.table.ElzaTable;

import java.util.Objects;

/**
 * Implementace {@link cz.tacr.elza.api.ArrItemJsonTable}
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ArrItemJsonTable extends ArrItemData implements cz.tacr.elza.api.ArrItemJsonTable<ElzaTable> {

    private ElzaTable value;

    @Override
    public ElzaTable getValue() {
        return value;
    }

    @Override
    public void setValue(final ElzaTable value) {
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
        ArrItemJsonTable that = (ArrItemJsonTable) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
