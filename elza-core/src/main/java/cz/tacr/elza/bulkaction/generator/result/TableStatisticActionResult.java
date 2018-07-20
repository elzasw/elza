package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.service.OutputItemConnector;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.TableStatisticAction}
 */
public class TableStatisticActionResult extends ActionResult {

    private String columnCode;

    private String columnDataType;

    private ElzaTable table;

    private String itemType;

    public String getColumnCode() {
        return columnCode;
    }

    public void setColumnCode(String columnCode) {
        this.columnCode = columnCode;
    }

    public String getColumnDataType() {
        return columnDataType;
    }

    public void setColumnDataType(String columnDataType) {
        this.columnDataType = columnDataType;
    }

    public ElzaTable getTable() {
        return table;
    }

    public void setTable(ElzaTable table) {
        this.table = table;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void createOutputItems(OutputItemConnector connector) {
        if (table == null) {
            return;
        }
        ItemType rsit = connector.getItemTypeByCode(itemType);
        connector.addTableItem(table, rsit, null);
    }
}
