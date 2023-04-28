package cz.tacr.elza.drools.model.item;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;

public class BoolItem extends AbstractItem {

    private Boolean value;

    public BoolItem(final Integer id, final ItemType itemType, final RulItemSpec itemSpec, final Boolean value) {
        super(id, itemType, itemSpec);
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }
}
