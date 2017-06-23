package cz.tacr.elza.print.item;

import org.apache.commons.lang.StringUtils;

/**
 * Text value for print
 *  
 */
public class ItemText extends AbstractItem {
	
	String value;

    public ItemText(final String value) {
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
