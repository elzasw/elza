package cz.tacr.elza.print.item;

import org.apache.commons.lang.StringUtils;

/**
 * Item string
 * 
 */
public class ItemString extends AbstractItem {
	
	String value;

    public ItemString(final String value) {
        super();
        
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
