package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemJsonTable;
import cz.tacr.elza.print.item.ItemType;

import java.util.List;

public class JsonTableItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.JSON_TABLE) {
            return null;
        }
        ArrDataJsonTable data = (ArrDataJsonTable) item.getData();

        return new ItemJsonTable((List<ElzaColumn>) itemType.getViewDefinition(), data.getValue());
    }
}
