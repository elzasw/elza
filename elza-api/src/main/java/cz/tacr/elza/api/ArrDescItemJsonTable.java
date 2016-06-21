package cz.tacr.elza.api;

import cz.tacr.elza.api.table.ElzaTable;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public interface ArrDescItemJsonTable<N extends ArrNode, T extends ElzaTable> extends ArrDescItem<N> {

    T getValue();

    void setValue(T value);
}
