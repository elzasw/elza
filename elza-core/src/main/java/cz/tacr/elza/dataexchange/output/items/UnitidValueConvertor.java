package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemStringImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;

public class UnitidValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemStringImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataUnitid.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataUnitid unitid = (ArrDataUnitid) data;
        DescriptionItemStringImpl item = new DescriptionItemStringImpl();
        item.setV(unitid.getUnitId());
        return item;
    }
}
