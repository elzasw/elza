package cz.tacr.elza.api;

import cz.tacr.elza.api.table.ElzaTable;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public interface ArrItemJsonTable<ET extends ElzaTable> extends ArrItemData {

    ET getValue();

    void setValue(ET value);
}
