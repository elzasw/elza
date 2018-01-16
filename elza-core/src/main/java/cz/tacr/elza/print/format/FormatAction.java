package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

/**
 * Interface for one formatting action
 */
public interface FormatAction {

    /**
     * Method to format items
     *
     * @param ctx Formatting context. Result is stored in this context
     * @param items List of items
     */
    void format(FormatContext ctx, List<Item> items);
}
