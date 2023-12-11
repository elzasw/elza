package cz.tacr.elza.print.format;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
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

    /**
     * Flag to append item type title.
     * 
     * This is useful to write only specification title
     */
    private boolean appendItemTypeTitle = true;

    /**
     * Optional other title which can overried default item type title
     */
	private String otherTitle;

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

    public ValueWithTitleFormatter setTitleLowerCase(boolean titleLowerCase) {
        this.titleLowerCase = titleLowerCase;
        return this;
    }

    public boolean isAppendItemTypeTitle() {
        return appendItemTypeTitle;
    }

    public ValueWithTitleFormatter setAppendItemTypeTitle(boolean appendItemTypeTitle) {
        this.appendItemTypeTitle = appendItemTypeTitle;
        return this;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

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
        if (firstItem && appendItemTypeTitle) {
            // get name
            String name = otherTitle!=null?otherTitle:item.getType().getName();

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
            String specName = ctx.getSpecName(spec);

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

	public void setOtherTitle(final String title) {
		this.otherTitle = title;		
	}
}
