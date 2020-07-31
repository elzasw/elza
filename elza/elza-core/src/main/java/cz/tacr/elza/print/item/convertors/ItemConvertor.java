package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.Item;

public interface ItemConvertor {

    cz.tacr.elza.print.item.Item convert(Item item, ItemConvertorContext context);

}
