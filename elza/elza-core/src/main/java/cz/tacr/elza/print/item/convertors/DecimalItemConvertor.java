package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemType;

public class DecimalItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.DECIMAL) {
            return null;
        }
        ArrDataDecimal data = (ArrDataDecimal) item.getData();

        return new ItemDecimal(data.getValue());
    }

}
