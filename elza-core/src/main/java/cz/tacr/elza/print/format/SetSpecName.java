package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class SetSpecName implements FormatAction {

    private final String code;
    private final String name;

    public SetSpecName(final String code, final String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        ctx.setSpecName(code, name);
    }

}
