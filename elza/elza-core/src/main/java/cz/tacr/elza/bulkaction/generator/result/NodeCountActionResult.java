package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.service.OutputItemConnector;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.NodeCountAction}
 */
public class NodeCountActionResult extends ActionResult {

    private Integer count;

    private String itemType;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void createOutputItems(OutputItemConnector connector) {
        if (count == null) {
            return;
        }
        ItemType rsit = connector.getItemTypeByCode(itemType);
        connector.addIntItem(count.intValue(), rsit, null);
    }
}
