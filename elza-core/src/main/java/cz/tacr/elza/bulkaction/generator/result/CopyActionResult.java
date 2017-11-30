package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemData;

import java.util.List;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.CopyAction}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class CopyActionResult extends ActionResult {

    private String itemType;

    private List<ArrItem> dataItems;

    public List<ArrItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(final List<ArrItem> dataItems) {
        this.dataItems = dataItems;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }
}
