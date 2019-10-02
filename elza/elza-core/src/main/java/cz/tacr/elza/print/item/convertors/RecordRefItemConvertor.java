package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemType;

public class RecordRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.RECORD_REF) {
            return null;
        }
        ArrDataRecordRef data = (ArrDataRecordRef) item.getData();
        Record record = context.getRecord(data.getRecord());

        return new ItemRecordRef(record);
    }
}
