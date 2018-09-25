package cz.tacr.elza.print.format;

import java.util.List;

import org.springframework.util.StringUtils;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Format value of one item.
 */
public class ValueFormatter implements FormatAction {
    /**
     * Code of item to format
     */
    private final String itemType;

    ValueFormatter(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        for (Item item : items) {
            if (item.getType().getCode().equals(itemType)) {
                formatItem(ctx, item);
            }
        }

    }

    /**
     * Format one item
     *
     * @param ctx
     * @param item
     */
    private void formatItem(FormatContext ctx, Item item) {
        String value = item.getSerializedValue();

        ItemSpec spec = item.getSpecification();
        if (spec != null) {
            // write value with specification
        	SpecTitleSource specTitleSource = ctx.getSpecTitleSource();
        	String specName = specTitleSource.getValue(spec).toLowerCase();

            ctx.appendSpecWithValue(specName, value);
        } else {
            // write value without specification
            ctx.appendValue(value);
        }
    }
}
