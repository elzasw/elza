package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUriRef;

public class UriRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.URI_REF) {
            return null;
        }
        ArrDataUriRef data = HibernateUtils.unproxy(item.getData());
        ArrNode linkedNode = data.getArrNode();

        return new ItemUriRef(data.getSchema(), data.getUriRefValue(), data.getDescription(),
                linkedNode, context);
    }
}
