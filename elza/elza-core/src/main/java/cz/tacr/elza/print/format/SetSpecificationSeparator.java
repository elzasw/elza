package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * Set separator between specification and its value.
 */
public class SetSpecificationSeparator implements FormatAction {

    /**
     * Separator between specification and value
     */
    private final String specSeparator;

    public SetSpecificationSeparator(String specSeparator) {
        this.specSeparator = specSeparator;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setSpecificationSeparator(specSeparator);
    }
}
