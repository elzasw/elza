package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class SetGroupBySpec implements FormatAction {

    private boolean groupBySpec;

    public SetGroupBySpec(final boolean groupBySpec) {
        this.groupBySpec = groupBySpec;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setGroupBySpec(groupBySpec);
    }

}
