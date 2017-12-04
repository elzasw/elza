package cz.tacr.elza.print.item;

import cz.tacr.elza.print.UnitDate;

/**
 * Unit date
 *
 */
public class ItemUnitdate extends AbstractItem {

    private final UnitDate value;

    public ItemUnitdate(final UnitDate value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        return value.serialize();
    }

    @Override
    protected UnitDate getValue() {
        return value;
    }
}
