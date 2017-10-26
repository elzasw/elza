package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemStringImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

public class TextValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemStringImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataText.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataText text = (ArrDataText) data;
        DescriptionItemStringImpl item = new DescriptionItemStringImpl();
        item.setV(text.getValue());
        return item;
    }
}
