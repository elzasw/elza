package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * Action to set separator between two items.
 */
public class SetItemSeparator implements FormatAction {

    /**
     * Separator between two items
     */
    private final String itemSeparator;

    public SetItemSeparator(String itemSeparator) {
        this.itemSeparator = itemSeparator;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setItemSeparator(itemSeparator);
    }
}
