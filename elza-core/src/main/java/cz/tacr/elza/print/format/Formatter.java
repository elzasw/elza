package cz.tacr.elza.print.format;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.item.Item;

/**
 * Format node as string
 * 
 * Class allows to customize formatter object.
 *
 */
public class Formatter {
	
	/**
	 * List of formatting actions
	 */
	List<FormatAction> actions = new ArrayList<>();
	
	/**
	 * Add formatting action
	 * @param action
	 * @return Return this formatter
	 */
	Formatter addAction(FormatAction action)
	{
		actions.add(action);
		return this;
	}
	
	/**
	 * Add value without any other info 
	 * @param itemType Item type
	 * @return
	 */
	public Formatter addValue(String itemType)
	{
		return addAction(new ValueFormatter(itemType));
	}
	
	/**
	 * Add value of multiple types without any other info
	 * @param itemType Item types
	 * @return
	 */
	public Formatter addValue(String itemTypes[])
	{
		for(String itemType: itemTypes)
		{
			addAction(new ValueFormatter(itemType));
		}
		return this;
	}
	
	/**
	 * Add packet value
	 * @param itemType Item type
	 * @param formatType Packet format
	 * @return
	 */
	public Formatter addPacketValue(String itemType, Packet.FormatType formatType)
	{
		return addAction(new PacketFormatter(itemType, formatType));
	}
	
	/**
	 * Add specification and value for given type
	 * @param itemType
	 * @return
	 */
	public Formatter addValueWithTitle(String itemType) {
		return addAction(new ValueWithTitleFormatter(itemType));
	}

	/**
	 * Add specification and value for given types
	 * @param itemTypes
	 * @return
	 */
	public Formatter addValueWithTitle(String itemTypes[]) {
		for(String itemType: itemTypes)
		{
			addAction(new ValueWithTitleFormatter(itemType));
		}
		return this;		
	}
	
	public Formatter beginBlock(){
		return addAction(new BeginBlockFormatter());
	}

	public Formatter endBlock(){
		return addAction(new EndBlockFormatter());
	}
	
	/**
	 * Set separator for specification
	 * @param specSeparator Separator for specification
	 * @return
	 */
	public Formatter setSpecSeparator(String specSeparator) {
		return addAction(new SetSpecificationSeparator(specSeparator));
	}
	
	/**
	 * Set separator between title and following value
	 * @param titleSeparator
	 * @return
	 */
	public Formatter setTitleSeparator(String titleSeparator) {
		return addAction(new SetTitleSeparator(titleSeparator));
	}
	
	/**
	 * Set separator between two items
	 * @param itemSeparator
	 * @return
	 */
	public Formatter setItemSeparator(String itemSeparator) {
		return addAction(new SetItemSeparator(itemSeparator));
	}
	
	/**
	 * Set separator for new block
	 * @param itemSeparator
	 * @return
	 */
	public Formatter setBlockSeparators(String beginBlockSeparator, String endBlockSeparator) {
		return addAction(new SetBlockSeparators(beginBlockSeparator, endBlockSeparator));
	}
	
	/**
	 * Format items from node
	 * @param node Node to be formatted
	 * @return Return string
	 */
	public String format(Node node)
	{
		FormatContext ctx = new FormatContext();
		List<Item> items = node.getItems();
		
		for(FormatAction action: actions)
		{
			action.format(ctx, items);
		}
		
		return ctx.getResult();
	}
	
}
