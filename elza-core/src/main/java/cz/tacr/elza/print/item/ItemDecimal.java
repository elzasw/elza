package cz.tacr.elza.print.item;

import java.math.BigDecimal;

/**
 * Decimal number for print
 * 
 */
public class ItemDecimal extends AbstractItem {
	
	BigDecimal value;

    public ItemDecimal(final BigDecimal value) {
        super();
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
