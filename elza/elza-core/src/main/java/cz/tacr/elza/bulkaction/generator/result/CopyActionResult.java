package cz.tacr.elza.bulkaction.generator.result;

import java.util.List;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.service.OutputItemConnector;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.CopyAction}
 */
public class CopyActionResult extends ActionResult {

    private List<ArrItem> dataItems;

    private String itemType;

    public List<ArrItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(List<ArrItem> dataItems) {
        this.dataItems = dataItems;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void createOutputItems(OutputItemConnector connector) {
        if (dataItems == null) {
            return;
        }
        ItemType rsit = connector.getItemTypeByCode(itemType);
        connector.addItems(dataItems, rsit);
    }
}
