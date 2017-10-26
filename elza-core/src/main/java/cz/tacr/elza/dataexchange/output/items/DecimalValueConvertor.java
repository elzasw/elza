package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemDecimalImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;

public class DecimalValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemDecimalImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataDecimal.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataDecimal decimalValue = (ArrDataDecimal) data;
        DescriptionItemDecimalImpl item = new DescriptionItemDecimalImpl();
        item.setV(decimalValue.getValue());
        return item;
    }
}
