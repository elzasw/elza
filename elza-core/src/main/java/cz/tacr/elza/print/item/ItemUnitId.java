package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 * 
 */
public class ItemUnitId extends AbstractItem {
	
	String value;

    public ItemUnitId(final NodeId nodeId, final String value) {
        super(nodeId);
        
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return value;
    }

	@Override
	public Object getValue() {
		return value;
	}
}
