package cz.tacr.elza.drools.model.item;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;

public class DoubleItem extends AbstractItem {

    private Double value;

    public DoubleItem(final Integer id, final ItemType itemType, final RulItemSpec itemSpec, final Double value) {
        super(id, itemType, itemSpec);
        this.value = value;
    }

    public Double getValue() {
        return value;
    }
}
