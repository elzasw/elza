package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.UnitDate;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 * 
 */
public class ItemUnitdate extends AbstractItem {
	
	UnitDate value;

    public ItemUnitdate(final NodeId nodeId, final UnitDate value) {
        super(nodeId);
        
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
}
