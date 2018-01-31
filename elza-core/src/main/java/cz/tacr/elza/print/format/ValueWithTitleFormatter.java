package cz.tacr.elza.print.format;

import java.util.List;

import org.springframework.util.StringUtils;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Append specification and value.
 */
public class ValueWithTitleFormatter implements FormatAction {

    /**
     * Code of item type to be formatted
     */
    private final String itemType;

    /**
     * Flag to write title in lower case
     */
    private boolean titleLowerCase = true;

    public ValueWithTitleFormatter(String itemType) {
        this.itemType = itemType;
    }

    public boolean isTitleLowerCase() {
        return titleLowerCase;
    }

    public void setTitleLowerCase(boolean titleLowerCase) {
        this.titleLowerCase = titleLowerCase;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        boolean firstItem = true;
        for (Item item : items) {
            if (item.getType().getCode().equals(itemType)) {
                if (formatItem(firstItem, ctx, item)) {
                    firstItem = false;
                }
            }
        }
    }

    /**
     * Format single item
     *
     * @param ctx
     * @param item
     * @return Return true if item was added
     */
    private boolean formatItem(final boolean firstItem, FormatContext ctx, Item item) {
        String value = item.getSerializedValue();

        // Append title
        String oldItemSeparator = null;
        if (firstItem) {
            // get name
            String name = item.getType().getShortcut();
            if (StringUtils.isEmpty(name)) {
                name = item.getType().getName();
            }
            // convert name
            if (titleLowerCase) {
                name = name.toLowerCase();
            }
            ctx.appendValue(name);
            oldItemSeparator = ctx.getItemSeparator();
            ctx.setItemSeparator(ctx.getTitleSeparator());
        }

        // Append value
        ItemSpec spec = item.getSpecification();
        if (spec != null) {
            // get specification
            String specName = spec.getShortcut();
            if (StringUtils.isEmpty(specName)) {
                specName = spec.getName();
            }
            // convert name
            if (titleLowerCase) {
                specName = specName.toLowerCase();
            }
            // write value with specification
            ctx.appendSpecWithValue(specName, value);
        } else {
            // write value without specification
            ctx.appendValue(value);
        }
        if (firstItem) {
            ctx.setItemSeparator(oldItemSeparator);
        }
        return true;
    }
}
