package cz.tacr.elza.dataexchange.output.filters;

import cz.tacr.elza.core.data.ItemType;

public class ReplaceItem {

    final private ItemType source;

    final private ItemType target;

    public ReplaceItem(ItemType source, ItemType target) {
        this.source = source;
        this.target = target;
    }

    public ItemType getSource() {
        return source;
    }

    public ItemType getTarget() {
        return target;
    }
}