package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.RuleSystemItemType;
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
        if (text == null) {
            return;
        }
        RuleSystemItemType rsit = connector.getItemTypeByCode(itemType);
        connector.addStringItem(text, rsit, null);
    }
}
