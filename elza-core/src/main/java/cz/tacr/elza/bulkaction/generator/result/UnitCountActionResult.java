package cz.tacr.elza.bulkaction.generator.result;

/**
 * Výsledek z akce {@link cz.tacr.elza.bulkaction.generator.multiple.NodeCountAction}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class UnitCountActionResult extends ActionResult {

    private String itemType;

    private Integer count;

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }
}
