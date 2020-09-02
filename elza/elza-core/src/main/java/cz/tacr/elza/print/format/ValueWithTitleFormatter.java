package cz.tacr.elza.print.format;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
    
    private final Formatter formatter;

    /**
     * Flag to write title in lower case
     */
    private boolean titleLowerCase = true;

    public ValueWithTitleFormatter(String itemType) {
        this(itemType, null);
    }
    
    public ValueWithTitleFormatter(String itemType, Formatter formatter) {
        this.itemType = itemType;
        this.formatter = formatter;
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
            	String postfix = null;
            	if(formatter != null) {
            		 postfix = formatter.format(items);
            	}
                if (formatItem(firstItem, ctx, item, postfix)) {
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
    private boolean formatItem(final boolean firstItem, FormatContext ctx, Item item, String postfix) {
        String value = item.getSerializedValue();
        
        if (StringUtils.isNotBlank(postfix)) {
        	value = new StringBuilder().append(value).append(postfix).toString();
        }

        // Append title
        if (firstItem) {
            // get name
            String name = item.getType().getName();
            if (StringUtils.isEmpty(name)) {
                name = item.getType().getName();
            }
            // convert name
            if (titleLowerCase) {
                name = name.toLowerCase();
            }
            
            String oldItemSeparator = ctx.getItemSeparator();
            ctx.setItemSeparator(ctx.getTitleSeparator());
            ctx.appendValue(name);
            ctx.setItemSeparator(oldItemSeparator);
        }

        // Append value
        ItemSpec spec = item.getSpecification();
        if (spec != null) {
            // get specification
        	SpecTitleSource specTitleSource = ctx.getSpecTitleSource();
        	String specName = specTitleSource.getValue(spec);

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
        
        return true;
    }
}
