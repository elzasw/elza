package cz.tacr.elza.drools.model;

import java.util.Collections;
import java.util.List;

import cz.tacr.elza.drools.model.item.AbstractItem;

/**
 * Collection of expected items
 */
public class ExpectedItems {
    private final List<AbstractItem> items;

    public ExpectedItems() {
        items = Collections.emptyList();
    }

    public ExpectedItems(final List<AbstractItem> items) {
        this.items = items;
    }

    public List<AbstractItem> getItems() {
        return items;
    }
}
