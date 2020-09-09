package cz.tacr.elza.print.format;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;
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

    protected FormatContext createFormatCtx() {
        return new FormatContext();
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
	 * @param itemTypes Item types
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
	 * Add specification and value for given type
	 * @param itemType
	 * @return
	 */
	public Formatter addValueWithTitle(String itemType) {
		return addAction(new ValueWithTitleFormatter(itemType));
	}
	
	/**
	 * Add specification and value for given type with additional format
	 * @param itemType
	 * @return
	 */
	public Formatter addValueWithTitle(String itemType, Formatter formatter) {
		return addAction(new ValueWithTitleFormatter(itemType, formatter));
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

	public Formatter setSpecTitleSource(SpecTitleSource source) {
		return addAction(new SetSpecificationTitleSource(source));
	}
	/**
	 * Set separator for specification
	 * @param specSeparator Separator for specification
	 * @return
	 */
	public Formatter setSpecSeparator(String specSeparator) {
		return addAction(new SetSpecificationSeparator(specSeparator));
	}

	public Formatter setSpecFormat(String prefix, String postfix, boolean afterValue) {
		return addAction(new SetSpecificationFormat(prefix, postfix, afterValue));
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
	 * 
	 * Separator is added only if block is not last or first
	 * @param beginBlockSeparator
	 * @param endBlockSeparator
	 * @return
	 */
	public Formatter setBlockSeparators(String beginBlockSeparator, String endBlockSeparator) {
	    return setBlockSeparators(beginBlockSeparator, endBlockSeparator, false, false);
	}

	/**
	 * Set separator for new block
	 * 
	 * @param beginBlockSeparator
	 * @param endBlockSeparator
	 * @param useAlways If true beginBlockSeparator and endBlockSeparator are added always. If false
	 *   beginBlockSeparator is added only if item is not first and endBlockSeparator is added only
	 *   if item is not last
	 * @return formatter
	 */
    public Formatter setBlockSeparators(String beginBlockSeparator, String endBlockSeparator, boolean useAlways) {
        return setBlockSeparators(beginBlockSeparator, endBlockSeparator, useAlways, useAlways);
    }

    /**
     * Set separator for new block
     * 
     * @param beginBlockSeparator
     * @param endBlockSeparator
     * @param useBeginSeparatorAlways If true beginBlockSeparator is added always. If false
     *   beginBlockSeparator is added only if item is not first
     * @param useEndSeparatorAlways If true endBlockSeparator is added always. If false
     *   endBlockSeparator is added only if item is not last
     * @return formatter
     */
    public Formatter setBlockSeparators(String beginBlockSeparator, String endBlockSeparator, 
                                        boolean useBeginSeparatorAlways,
                                        boolean useEndSeparatorAlways) {
        return addAction(new SetBlockSeparators(beginBlockSeparator, endBlockSeparator, 
                useBeginSeparatorAlways, useEndSeparatorAlways));
    }

    public Formatter setGroupBySpec(boolean groupBySpec) {
        return addAction(new SetGroupBySpec(groupBySpec));
    }

    /**
     * Set group format
     * 
     * @param itemSeparator
     * @return
     */
    public Formatter setGroupFormat(String itemSeparator) {
        return addAction(new SetGroupFormat(itemSeparator));
    }

    /**
	 * Format items from node
	 * @param node Node to be formatted
	 * @return Return string
	 */
	public String format(Node node)
	{
		List<Item> items = node.getItems();

		return format(items);
	}
	
	/**
	 * Format items from output
	 * 
	 * @param node
	 *            Output to be formatted
	 * @return Return string
	 */
	public String format(Output output) {
		List<Item> items = output.getItems();

		return format(items);
	}

	/**
	 * Format items
	 * 
	 * @param items
	 *            to be formatted
	 * @return Return string
	 */
	public String format(List<Item> items) {
        FormatContext ctx = createFormatCtx();

		for (FormatAction action : actions) {
			action.format(ctx, items);
		}

		return ctx.getResult();
	}

    /**
     * Replace unsupported characters
     * 
     * @param value
     * @return
     */
    public String format(String value) {
        FormatContext ctx = createFormatCtx();
        ctx.appendValue(value);
        return ctx.getResult();
    }
}
