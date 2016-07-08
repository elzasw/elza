package cz.tacr.elza.api;


import cz.tacr.elza.api.table.ElzaTable;

import java.io.Serializable;


/**
 * Hodnota atributu archivního popisu typu JsonTable.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public interface ArrDataJsonTable<T extends ElzaTable> extends Serializable {


    T getValue();


    void setValue(T value);
}
