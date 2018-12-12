package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.schema.v2.DescriptionItemDecimal;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class DecimalValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemDecimal convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataDecimal.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataDecimal decimalValue = (ArrDataDecimal) data;
        DescriptionItemDecimal item = objectFactory.createDescriptionItemDecimal();
        item.setV(decimalValue.getValue());
        return item;
    }
}
