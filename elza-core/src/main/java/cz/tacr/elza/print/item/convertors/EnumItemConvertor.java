package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.Item;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemEnum;
import cz.tacr.elza.print.item.ItemType;

public class EnumItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.ENUM) {
            return null;
        }
        ArrData data = HibernateUtils.unproxy(item.getData());
        Validate.isTrue(data.getClass() == ArrDataNull.class);
        // integrity check - spec must be set for defined item
        Validate.notNull(item.getItemSpecId(), "Položka bez uvedení specifikace: %d", data.getDataId());

        return new ItemEnum();
    }
}
