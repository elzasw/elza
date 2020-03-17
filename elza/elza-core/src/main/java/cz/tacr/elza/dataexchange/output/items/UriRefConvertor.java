package cz.tacr.elza.dataexchange.output.items;

import cz.tacr.elza.dataexchange.output.DEExportException;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;
import cz.tacr.elza.schema.v2.ObjectFactory;
import org.apache.commons.lang.Validate;

public class UriRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass()== ArrDataUriRef.class, "Invalid data type, dataId:", data.getDataId());

        //TODO : zpracovat případné kontroly podmínek

        DescriptionItemUriRef item = objectFactory.createDescriptionItemUriRef();
        item.setValue(((ArrDataUriRef) data).getValue());


        return item;

    }
}
