package cz.tacr.elza.bulkaction.generator;

/**
 * Dodatečné nastavení funkce.
 */
public class PersistentSortRunConfig {

    /** Příznak zda řadit vzestupně. */
    private boolean asc;

    /** Příznak zda řadit i potomky. */
    private boolean sortChildren;

    /** Kód typu atributu. */
    private String itemTypeCode;

    /** Kód specifikace typu atributu. */
    private String itemSpecCode;

    public boolean isAsc() {
        return asc;
    }

    public void setAsc(boolean asc) {
        this.asc = asc;
    }

    public boolean isSortChildren() {
        return sortChildren;
    }

    public void setSortChildren(boolean sortChildren) {
        this.sortChildren = sortChildren;
    }

    public String getItemTypeCode() {
        return itemTypeCode;
    }

    public void setItemTypeCode(String itemTypeCode) {
        this.itemTypeCode = itemTypeCode;
    }

    public String getItemSpecCode() {
        return itemSpecCode;
    }

    public void setItemSpecCode(String itemSpecCode) {
        this.itemSpecCode = itemSpecCode;
    }
}
