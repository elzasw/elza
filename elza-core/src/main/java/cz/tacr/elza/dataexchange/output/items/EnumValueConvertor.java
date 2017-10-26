package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemEnumImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;

public class EnumValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemEnumImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataNull.class, "Invalid data type, dataId:", data.getDataId());

        return new DescriptionItemEnumImpl();
    }
}
