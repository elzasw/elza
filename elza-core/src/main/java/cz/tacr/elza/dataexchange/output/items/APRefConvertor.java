package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class APRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemAPRef convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataRecordRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataRecordRef apRef = (ArrDataRecordRef) data;
        DescriptionItemAPRef item = objectFactory.createDescriptionItemAPRef();
        item.setApid(apRef.getRecordId().toString());
        return item;
    }
}
