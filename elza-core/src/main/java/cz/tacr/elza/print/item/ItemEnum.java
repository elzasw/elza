package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;

/**
 * Enumerated Item for outputs
 * 
 * This type is without value
 */
public class ItemEnum extends AbstractItem {

    private ItemEnum(final NodeId nodeId) {
        super(nodeId);
    }

    @Override
    public String serializeValue() {
        return getSpecification().getName();
    }

    @Override
    public String serialize() {
        return serializeValue();
    }
    
    @Override
    public Object getValue() {
    	return "";
    }

	public static AbstractItem newInstance(NodeId nodeId) {
		return new ItemEnum(nodeId);
	}

}
