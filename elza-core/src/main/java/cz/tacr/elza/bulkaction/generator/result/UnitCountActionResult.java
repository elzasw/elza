package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.NodeCountAction}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class UnitCountActionResult extends ActionResult {

    private String itemType;

    private ElzaTable table;

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
