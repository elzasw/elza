package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class JsonTableConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataJsonTable.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataJsonTable jsonTable = (ArrDataJsonTable) data;
        DescriptionItemString item = objectFactory.createDescriptionItemString();
        item.setV(jsonTable.getJsonValue());
        return item;
    }
}
