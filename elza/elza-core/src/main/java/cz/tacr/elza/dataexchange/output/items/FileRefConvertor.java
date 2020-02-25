package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.schema.v2.DescriptionItemFileRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class FileRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemFileRef convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataFileRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataFileRef fileRef = (ArrDataFileRef) data;
        DescriptionItemFileRef item = objectFactory.createDescriptionItemFileRef();
        item.setFid(fileRef.getFileId().toString());
        return item;
    }

}
