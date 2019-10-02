package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.service.OutputItemConnector;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.NodeCountAction}
 */
public class UnitCountActionResult extends ActionResult {

    private ElzaTable table;

    private String itemType;

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
