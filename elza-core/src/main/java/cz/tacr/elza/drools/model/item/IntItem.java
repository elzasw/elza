package cz.tacr.elza.drools.model.item;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;

public class IntItem extends AbstractItem {

    private Integer value;

    public IntItem(final Integer id, final ItemType itemType, final RulItemSpec itemSpec, final Integer value) {
        super(id, itemType, itemSpec);
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
