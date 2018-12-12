package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class EnumValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemEnum convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataNull.class, "Invalid data type, dataId:", data.getDataId());

        return objectFactory.createDescriptionItemEnum();
    }
}
