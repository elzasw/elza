package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.IntItem;
import cz.tacr.elza.print.item.Item;

public interface ItemConvertor {

    Item convert(IntItem item, ItemConvertorContext context);

}
