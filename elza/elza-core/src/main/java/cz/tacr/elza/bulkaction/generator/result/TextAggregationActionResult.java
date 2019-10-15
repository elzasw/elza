package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.service.OutputItemConnector;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.TextAggregationAction}
 */
public class TextAggregationActionResult extends ActionResult {

    private String text;

    private String itemType;

    /**
     * Flag if value/item should be stored in output
     */
    private boolean createInOutput;

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

    public boolean isCreateInOutput() {
        return createInOutput;
    }

    public void setCreateInOutput(boolean createInOutput) {
		this.createInOutput = createInOutput;
	}

    @Override
    public void createOutputItems(OutputItemConnector connector) {
        if (text == null || !createInOutput) {
            return;
        }
        ItemType rsit = connector.getItemTypeByCode(itemType);
        connector.addStringItem(text, rsit, null);
    }
}
