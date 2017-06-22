package cz.tacr.elza.print.format;

import java.util.List;

import org.springframework.util.StringUtils;

import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.Packet.FormatType;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Packet formatter
 *
 */
public class PacketFormatter implements FormatAction {

	/**
	 * Code of item to format
	 */
	String itemType;
	
	/**
	 * Format type
	 */
	FormatType formatType;

	public PacketFormatter(String itemType, FormatType formatType) {
		this.itemType = itemType;
		this.formatType = formatType;
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
		Packet packet = item.getValue(Packet.class);
		String value = packet.formatAsString(formatType);
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
