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
    
    private boolean useBeginSeparatorAlways = true;
    
    private boolean useEndSeparatorAlways = true;
    
    public SetBlockSeparators(final String beginBlockSeparator, final String endBlockSeparator) {
        this.beginBlockSeparator = beginBlockSeparator;
        this.endBlockSeparator = endBlockSeparator;
    }

    public SetBlockSeparators(final String beginBlockSeparator, final String endBlockSeparator, 
                              final boolean useBeginSeparatorAlways, final boolean useEndSeparatorAlways) {
        this.beginBlockSeparator = beginBlockSeparator;
        this.endBlockSeparator = endBlockSeparator;
        this.useBeginSeparatorAlways = useBeginSeparatorAlways;
        this.useEndSeparatorAlways = useEndSeparatorAlways;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setBeginBlockSeparator(beginBlockSeparator, useBeginSeparatorAlways);
        ctx.setEndBlockSeparator(endBlockSeparator, useEndSeparatorAlways);
    }
}
