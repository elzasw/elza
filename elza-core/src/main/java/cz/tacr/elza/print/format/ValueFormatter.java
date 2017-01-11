package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;
import org.springframework.util.StringUtils;

/**
 * Format value of one item
 * @author Petr Pytelka
 *
 */
public class ValueFormatter
	implements FormatAction
{
	/**
	 * Code of item to format
	 */
	String itemType;
	
	ValueFormatter(String itemType)
	{
		this.itemType = itemType;
	}

	@Override
	public void format(FormatContext ctx, List<Item> items) {
		for(Item item: items) 
		{
			if(item.getType().getCode().equals(itemType)) {
				formatItem(ctx, item);
			}
		}
		
	}

	/**
	 * Format one item
	 * @param ctx
	 * @param item
	 */
	private void formatItem(FormatContext ctx, Item item) {
		String value = item.serializeValue();
		if(StringUtils.isEmpty(value)) {
			return;
		}
		ItemSpec spec = item.getSpecification();			
		if(spec!=null) {
			// write value with specification
			String specName = spec.getShortcut();
			if(StringUtils.isEmpty(specName)) {
				specName = spec.getName();
			}			
			ctx.appendSpecWithValue(specName, value);
		} else {
			// write value without specification
			ctx.appendValue(value);
		}		
	}
}
