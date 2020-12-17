package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class TextValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemString convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataText.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataText text = (ArrDataText) data;
        DescriptionItemString item = objectFactory.createDescriptionItemString();
        item.setV(text.getTextValue());
        return item;
    }
}
