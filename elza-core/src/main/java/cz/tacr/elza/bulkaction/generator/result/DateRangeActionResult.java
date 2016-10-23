package cz.tacr.elza.bulkaction.generator.result;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.DateRangeAction}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class DateRangeActionResult extends ActionResult {

    private String itemType;

    private String text;

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
}
