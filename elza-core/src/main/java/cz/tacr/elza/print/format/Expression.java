package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public interface Expression {

    boolean eval(FormatContext ctx, List<Item> items);

}
