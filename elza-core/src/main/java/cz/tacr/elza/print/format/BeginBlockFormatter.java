package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * Begin formatting block
 * @author Petr Pytelka
 *
 */
public class BeginBlockFormatter implements FormatAction {

	@Override
	public void format(FormatContext ctx, List<Item> items) {
		ctx.beginBlock();
	}

}
