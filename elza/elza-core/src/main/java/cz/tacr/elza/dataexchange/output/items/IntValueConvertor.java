package cz.tacr.elza.dataexchange.output.items;

import java.math.BigInteger;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class IntValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemInteger convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataInteger.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataInteger intValue = (ArrDataInteger) data;
        DescriptionItemInteger item = objectFactory.createDescriptionItemInteger();
        item.setV(BigInteger.valueOf(intValue.getIntegerValue()));
        return item;
    }
}
