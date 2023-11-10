package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemType;

public class DecimalItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.DECIMAL) {
            return null;
        }
        ArrDataDecimal data = HibernateUtils.unproxy(item.getData());

        return new ItemDecimal(data.getValue());
    }

}
