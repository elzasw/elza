package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * Action to set block separators
 */
public class SetBlockSeparators implements FormatAction {

    /**
     * Sepatator beginning the block
     */
    private final String beginBlockSeparator;

    /**
     * Separator ending the block
     */
    private final String endBlockSeparator;

    public SetBlockSeparators(String beginBlockSeparator, String endBlockSeparator) {
        this.beginBlockSeparator = beginBlockSeparator;
        this.endBlockSeparator = endBlockSeparator;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setBeginBlockSeparator(beginBlockSeparator);
        ctx.setEndBlockSeparator(endBlockSeparator);
    }
}
