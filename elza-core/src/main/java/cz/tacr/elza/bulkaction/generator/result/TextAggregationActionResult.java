package cz.tacr.elza.bulkaction.generator.result;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.TextAggregationAction}
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 29.06.2016
 */
public class TextAggregationActionResult extends ActionResult {

    private String itemType;

    private String text;
    
    /**
     * Flag if value/item should be stored in output
     */
    private boolean createInOutput;

	public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }

    public boolean isCreateInOutput() {
		return createInOutput;
	}

    public void setCreateInOutput(final boolean createInOutput) {
		this.createInOutput = createInOutput; 
		
	}
}
