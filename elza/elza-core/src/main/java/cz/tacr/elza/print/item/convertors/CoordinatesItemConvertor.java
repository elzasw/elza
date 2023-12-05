package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemType;

public class CoordinatesItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.COORDINATES) {
            return null;
        }
        ArrDataCoordinates data = HibernateUtils.unproxy(item.getData());

        return new ItemCoordinates(data.getValue());
    }
}
