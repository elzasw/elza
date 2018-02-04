package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemInteger;
import cz.tacr.elza.print.item.ItemType;

public class IntegerItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.INT) {
            return null;
        }
        ArrDataInteger data = (ArrDataInteger) item.getData();

        return new ItemInteger(data.getValue());
    }

}
