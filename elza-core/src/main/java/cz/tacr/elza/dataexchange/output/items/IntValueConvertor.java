package cz.tacr.elza.dataexchange.output.items;

import java.math.BigInteger;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemIntegerImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;

public class IntValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemIntegerImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataInteger.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataInteger intValue = (ArrDataInteger) data;
        DescriptionItemIntegerImpl item = new DescriptionItemIntegerImpl();
        item.setV(BigInteger.valueOf(intValue.getValue()));
        return item;
    }
}
