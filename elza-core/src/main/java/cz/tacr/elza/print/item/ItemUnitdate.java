package cz.tacr.elza.print.item;

import cz.tacr.elza.print.UnitDate;

/**
 * Unit date
 * 
 */
public class ItemUnitdate extends AbstractItem {
	
	UnitDate value;

    public ItemUnitdate(final UnitDate value) {
        super();
        
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return value.serialize();
    }

    @Override
    public Object getValue() {
        return value;
    }

    public UnitDate getUnitDate() {
        return value;
    }
}
