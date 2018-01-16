package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemStringImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.schema.v2.DescriptionItem;

public class JsonTableConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItem convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataJsonTable.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataJsonTable jsonTable = (ArrDataJsonTable) data;
        DescriptionItemStringImpl item = new DescriptionItemStringImpl();
        item.setV(jsonTable.getJsonValue());
        return item;
    }
}
