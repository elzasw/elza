package cz.tacr.elza.print.item.convertors;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemRecordExtRef;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemType;

public class RecordRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.RECORD_REF) {
            return null;
        }
        ArrDataRecordRef data = (ArrDataRecordRef) item.getData();
        ApAccessPoint referencedRecord = data.getRecord();
        if (referencedRecord != null) {
            Record record = context.getRecord(referencedRecord);

            return new ItemRecordRef(record);
        } else {
            // reference to external record
            ApBinding apBinding = data.getBinding();
            Validate.notNull(apBinding);

            return new ItemRecordExtRef(apBinding);

        }
    }
}
