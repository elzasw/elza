package cz.tacr.elza.drools.model.item;

import cz.tacr.elza.domain.RulItemSpec;

public class Item extends AbstractItem {

    private String value;

    public Item(final Integer id, final cz.tacr.elza.core.data.ItemType itemType, final RulItemSpec itemSpec,
                final String value) {
        super(id, itemType, itemSpec);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
