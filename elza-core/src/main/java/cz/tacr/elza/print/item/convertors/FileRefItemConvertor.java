package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemFileRef;
import cz.tacr.elza.print.item.ItemType;

public class FileRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.FILE_REF) {
            return null;
        }
        ArrDataFileRef data = (ArrDataFileRef) item.getData();
        File file = context.getFile(data.getFile());

        return new ItemFileRef(file);
    }
}
