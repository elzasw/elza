package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.IntItem;
import cz.tacr.elza.print.Structured;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemStructuredRef;
import cz.tacr.elza.print.item.ItemType;

public class StructuredObjectRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(IntItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.STRUCTURED) {
            return null;
        }
        ArrDataStructureRef data = (ArrDataStructureRef) item.getData();
        Structured structObj = context.getStructured(data.getStructuredObject());

        return new ItemStructuredRef(structObj);
    }
}
