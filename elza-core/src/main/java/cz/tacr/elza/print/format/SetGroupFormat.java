package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class SetGroupFormat implements FormatAction {

    private String sameSpecItemSeparator;

    public SetGroupFormat(final String itemSeparator) {
        this.sameSpecItemSeparator = itemSeparator;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setGroupFormat(sameSpecItemSeparator);
    }

}
