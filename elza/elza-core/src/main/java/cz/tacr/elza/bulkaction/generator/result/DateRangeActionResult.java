package cz.tacr.elza.bulkaction.generator.result;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.service.OutputItemConnector;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.DateRangeAction}
 */
public class DateRangeActionResult extends ActionResult {

    private String text;

    private String itemType;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void createOutputItems(OutputItemConnector connector) {
    	// Do not store empty values
        if (StringUtils.isBlank(text)) {
            return;
        }
        ItemType rsit = connector.getItemTypeByCode(itemType);
        connector.addStringItem(text, rsit, null);
    }
}
