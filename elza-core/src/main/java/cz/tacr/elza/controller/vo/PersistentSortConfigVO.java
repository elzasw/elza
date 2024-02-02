package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.bulkaction.generator.PersistentSortRunConfig;

/**
 * VO pro dodatečné nastavení funkce.
 */
public class PersistentSortConfigVO {

    /** Uzly na které se aplikuje řazení. */
    List<Integer> nodeIds;

    /** Příznak zda řadit vzestupně. */
    private boolean asc;

    /** Příznak zda řadit i potomky. */
    private boolean sortChildren;

    /** Kód typu atributu. */
    private String itemTypeCode;

    /** Kód specifikace typu atributu. */
    private String itemSpecCode;

    public List<Integer> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<Integer> nodeIds) {
        this.nodeIds = nodeIds;
    }

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

    public PersistentSortRunConfig createEntity() {
    	PersistentSortRunConfig config = new PersistentSortRunConfig();
    	config.setAsc(asc);
    	config.setSortChildren(sortChildren);
    	config.setItemTypeCode(itemTypeCode);
    	config.setItemSpecCode(itemSpecCode);
    	return config;
    }
}
