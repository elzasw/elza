package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * Set separator between title and value
 * @author Petr Pytelka
 *
 */
public class SetTitleSeparator implements FormatAction {
	
	/**
	 * Separator between title and value
	 */
	String titleSeparator;

	public SetTitleSeparator(String titleSeparator) {
		this.titleSeparator = titleSeparator;
	}

	@Override
	public void format(FormatContext ctx, List<Item> items) {
		ctx.setTitleSeparator(titleSeparator);
	}

}
