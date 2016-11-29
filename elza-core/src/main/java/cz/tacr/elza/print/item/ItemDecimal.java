package cz.tacr.elza.print.item;

import java.math.BigDecimal;

import cz.tacr.elza.print.NodeId;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 * 
 */
public class ItemDecimal extends AbstractItem {
	
	BigDecimal value;

    public ItemDecimal(final NodeId nodeId, final BigDecimal value) {
        super(nodeId);
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return value.toString();
    }
    
    @Override
    public Object getValue() {
    	return value;
    }

}
