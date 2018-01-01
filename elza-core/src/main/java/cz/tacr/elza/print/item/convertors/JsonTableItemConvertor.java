package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemJsonTable;
import cz.tacr.elza.print.item.ItemType;

public class JsonTableItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.JSON_TABLE) {
            return null;
        }
        ArrDataJsonTable data = (ArrDataJsonTable) item.getData();

        return new ItemJsonTable(itemType.getTableDefinition(), data.getValue());
    }
}
