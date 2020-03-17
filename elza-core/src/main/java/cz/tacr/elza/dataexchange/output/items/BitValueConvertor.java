package cz.tacr.elza.dataexchange.output.items;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.schema.v2.DescriptionItemBit;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.ObjectFactory;
import org.apache.commons.lang.Validate;

import java.math.BigInteger;

public class BitValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemBit convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataBit.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataBit boolValue = (ArrDataBit) data;
        DescriptionItemBit item = objectFactory.createDescriptionItemBit();
        item.setValue(boolValue.isValue());
        return item;
    }
}
