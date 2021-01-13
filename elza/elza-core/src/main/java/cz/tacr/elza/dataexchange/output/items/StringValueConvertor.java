package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class StringValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemString convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataString.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataString string = (ArrDataString) data;
        DescriptionItemString item = objectFactory.createDescriptionItemString();
        item.setV(string.getStringValue());
        return item;
    }
}
