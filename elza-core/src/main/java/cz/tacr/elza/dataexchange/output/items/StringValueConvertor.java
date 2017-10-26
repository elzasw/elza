package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemStringImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;

public class StringValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemStringImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataString.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataString string = (ArrDataString) data;
        DescriptionItemStringImpl item = new DescriptionItemStringImpl();
        item.setV(string.getValue());
        return item;
    }
}
