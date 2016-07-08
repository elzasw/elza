package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.TableStatisticAction}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class TableStatisticActionResult extends ActionResult {

    private String itemType;

    private String columnCode;

    private String columnDataType;

    private ElzaTable table;

    public String getColumnCode() {
        return columnCode;
    }

    public void setColumnCode(final String columnCode) {
        this.columnCode = columnCode;
    }

    public String getColumnDataType() {
        return columnDataType;
    }

    public void setColumnDataType(final String columnDataType) {
        this.columnDataType = columnDataType;
    }

    public ElzaTable getTable() {
        return table;
    }

    public void setTable(final ElzaTable table) {
        this.table = table;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }
}
