package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemString;
import cz.tacr.elza.print.item.ItemType;

public class StringItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        String value = getDataValue(itemType.getDataType(), item.getData());

        return value != null ? new ItemString(value) : null;
    }

    private static String getDataValue(DataType dataType, ArrData data) {
        switch (dataType) {
            case UNITID:
                ArrDataUnitid unitid = (ArrDataUnitid) data;
                return unitid.getUnitId();
            case TEXT:
                ArrDataText text = (ArrDataText) data;
                return text.getValue();
            case STRING:
                ArrDataString str = (ArrDataString) data;
                return str.getValue();
            case FORMATTED_TEXT:
                ArrDataText ftext = (ArrDataText) data;
                return ftext.getValue();
            default:
                return null;
        }
    }
}
