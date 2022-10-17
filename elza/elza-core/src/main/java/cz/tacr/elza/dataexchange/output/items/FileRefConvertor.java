package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.writer.xml.EdxOutputHelper;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemFileRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class FileRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataFileRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataFileRef fileRef = (ArrDataFileRef) data;
        // Check if file is not deleted
        ArrFile arrFile = fileRef.getFile();
        if (arrFile.getDeleteChange() != null) {
            return EdxOutputHelper.getObjectFactory().createDescriptionItemUndefined();
        }

        DescriptionItemFileRef item = objectFactory.createDescriptionItemFileRef();
        item.setFid(fileRef.getFileId().toString());
        return item;
    }

}
