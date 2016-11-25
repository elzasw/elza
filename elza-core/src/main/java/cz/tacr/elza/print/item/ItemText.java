package cz.tacr.elza.print.item;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.print.NodeId;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 *  
 */
public class ItemText extends AbstractItem {
	
	String value;

    public ItemText(final NodeId nodeId, final String value) {
        super(nodeId);
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return StringUtils.trim(value);
    }

	@Override
	public Object getValue() {
		return value;
	}
}
