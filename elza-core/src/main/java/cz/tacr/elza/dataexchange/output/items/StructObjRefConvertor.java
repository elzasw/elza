package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemStructObjectRefImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;

public class StructObjRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemStructObjectRefImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataStructureRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataStructureRef structObjRef = (ArrDataStructureRef) data;
        DescriptionItemStructObjectRefImpl item = new DescriptionItemStructObjectRefImpl();
        item.setSoid(structObjRef.getStructureDataId().toString());
        return item;
    }
}
