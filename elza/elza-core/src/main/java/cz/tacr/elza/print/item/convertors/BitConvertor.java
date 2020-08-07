package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemBit;
import cz.tacr.elza.print.item.ItemType;

public class BitConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.BIT) {
            return null;
        }
        ArrDataBit data = (ArrDataBit) item.getData();

        return new ItemBit(data.getValueBoolean());
    }

}
