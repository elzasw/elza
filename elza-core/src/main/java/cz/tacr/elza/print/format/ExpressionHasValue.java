package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class ExpressionHasValue implements Expression {

    final private String itemType;

    public ExpressionHasValue(final String itemType) {
        this.itemType = itemType;
    }

    @Override
    public boolean eval(FormatContext ctx, List<Item> items) {
        for (Item item : items) {
            // compare itemType
            if (item.getType().getCode().equals(itemType)) {
                return true;
            }
        }
        return false;
    }

}
