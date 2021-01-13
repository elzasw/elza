package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class StructObjRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemStructObjectRef convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataStructureRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataStructureRef structObjRef = (ArrDataStructureRef) data;
        DescriptionItemStructObjectRef item = objectFactory.createDescriptionItemStructObjectRef();
        item.setSoid(structObjRef.getStructuredObjectId().toString());
        return item;
    }
}
