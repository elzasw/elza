package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.Item;

public interface ItemConvertor {

    Item convert(ArrItem item, ItemConvertorContext context);
}
