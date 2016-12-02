package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemEnum extends AbstractItem {
	
	String value;

    public ItemEnum(final NodeId nodeId, final String value) {
        super(nodeId);
        this.value = value;
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
    	return value;
    }

}
