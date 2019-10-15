package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * End formatting block
 */
public class EndBlockFormatter implements FormatAction {

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.endBlock();
    }
}
