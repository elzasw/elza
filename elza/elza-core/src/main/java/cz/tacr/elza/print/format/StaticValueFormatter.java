package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class StaticValueFormatter implements FormatAction {

    private String value;

    public StaticValueFormatter(final String value) {
        this.value = value;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        // write value without specification
        ctx.appendValue(value);
    }

}
