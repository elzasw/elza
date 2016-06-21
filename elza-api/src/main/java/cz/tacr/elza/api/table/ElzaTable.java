package cz.tacr.elza.api.table;

import java.util.List;

/**
 * Tabulka.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public interface ElzaTable<R extends ElzaRow> {


    List<R> getRows();

    void setRows(List<R> rows);

    void addRow(R row);
}
