package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class UnitidValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemString convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataUnitid.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataUnitid unitid = (ArrDataUnitid) data;
        DescriptionItemString item = objectFactory.createDescriptionItemString();
        item.setV(unitid.getUnitId());
        return item;
    }
}
