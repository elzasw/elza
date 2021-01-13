package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class UriRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass()== ArrDataUriRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataUriRef dataRef = (ArrDataUriRef) data;

        DescriptionItemUriRef item = objectFactory.createDescriptionItemUriRef();
        item.setUri(dataRef.getUriRefValue());
        item.setSchm(dataRef.getSchema());
        item.setLbl(dataRef.getDescription());

        return item;

    }
}
