package cz.tacr.elza.domain;

import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Implementace {@link cz.tacr.elza.api.ArrDescItemJsonTable}
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ArrDescItemJsonTable extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemJsonTable<ArrNode, ElzaTable> {

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
}
