package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.IntItem;
import cz.tacr.elza.print.UnitDate;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUnitdate;

public class UnitDateItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(IntItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.UNITDATE) {
            return null;
        }
        ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
        UnitDate unitDate = new UnitDate(data);

        return new ItemUnitdate(unitDate);
    }
}
