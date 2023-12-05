package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemFileRef;
import cz.tacr.elza.print.item.ItemType;

public class FileRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.FILE_REF) {
            return null;
        }
        ArrDataFileRef data = HibernateUtils.unproxy(item.getData());
        ArrFile dataFile = data.getFile();
        String name = dataFile.getName();
        File file = context.getFile(dataFile);

        return new ItemFileRef(file, name);
    }
}
