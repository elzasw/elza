package cz.tacr.elza.bulkaction.generator.result;

/**
 * Result of {@link cz.tacr.elza.bulkaction.generator.ContentMigration}.
 */
public class ContentMigrationResult extends ActionResult {

    /**
     * Number of items moved to their target item type.
     */
    private int movedItems;

    public int getMovedItems() {
        return movedItems;
    }

    public void setMovedItems(int movedItems) {
        this.movedItems = movedItems;
    }
}
