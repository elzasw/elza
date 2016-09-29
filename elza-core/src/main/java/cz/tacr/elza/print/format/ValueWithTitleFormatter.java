package cz.tacr.elza.print.format;

import java.util.List;

import org.jadira.usertype.spi.utils.lang.StringUtils;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Append specification and value
 * @author Petr Pytelka
 *
 */
public class ValueWithTitleFormatter implements FormatAction {
	
	/**
	 * Code of item type to be formatted
	 */
	String itemType;

	public ValueWithTitleFormatter(String itemType) {
		this.itemType = itemType;
	}

	@Override
	public void format(FormatContext ctx, List<Item> items) {
		boolean firstItem = true;
		for(Item item: items) 
		{
			if(item.getType().getCode().equals(itemType)) {
				if(formatItem(firstItem, ctx, item)) {
					firstItem = false;
				}
			}
		}
	}

	/**
	 * Format single item
	 * @param ctx
	 * @param item
	 * @return Return true if item was added
	 */
	private boolean formatItem(final boolean firstItem, FormatContext ctx, Item item) {
		String value = item.serializeValue();
		// Check if not empty
		if(StringUtils.isEmpty(value)) {
			return false;
		}
		
		// Append title
		String oldItemSeparator = null;
		if(firstItem) {
			String name = item.getType().getShortcut();
			if(StringUtils.isEmpty(name)) {
				name = item.getType().getName();
			}
			ctx.appendValue(name);
			oldItemSeparator = ctx.getItemSeparator();
			ctx.setItemSeparator(ctx.getTitleSeparator());
		}
			
		// Append value
		ItemSpec spec = item.getSpecification();			
		if(spec!=null) {
			String specName = spec.getShortcut();
			if(StringUtils.isEmpty(specName)) {
				specName = spec.getName();
			}
			// write value with specification
			ctx.appendSpecWithValue(specName, value);
		} else {
			// write value without specification
			ctx.appendValue(value);
		}
		if(firstItem) {
			ctx.setItemSeparator(oldItemSeparator);
		}
		return true;
	}
}
