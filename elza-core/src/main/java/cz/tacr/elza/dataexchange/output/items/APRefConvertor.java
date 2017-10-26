package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemAPRefImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;

public class APRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemAPRefImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataRecordRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataRecordRef apRef = (ArrDataRecordRef) data;
        DescriptionItemAPRefImpl item = new DescriptionItemAPRefImpl();
        item.setApid(apRef.getRecordId().toString());
        return item;
    }
}
