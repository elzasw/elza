package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.IntItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;

public abstract class AbstractItemConvertor implements ItemConvertor {

    protected ItemConvertorContext context;

    @Override
    public final Item convert(IntItem iItem, ItemConvertorContext context) {
        this.context = context; // prepare context for implementation

        ItemType itemType = context.getItemTypeById(iItem.getItemTypeId());

        AbstractItem item;
        try {
            item = convert(iItem, itemType);
        } catch (Exception e) {
            ArrData data = iItem.getData();
            throw new BusinessException(
                    "Failed to convert item, itemId = " + iItem.getItemId() + ", targetItemType=" + itemType.getCode()
                            + ", dataType=" + ((data == null) ? "null" : data.getClass().getCanonicalName()),
                    e, BaseCode.DB_INTEGRITY_PROBLEM);
        }
        // check if item was converter by this converter
        if (item == null) {
            return null;
        }
        // update common properties
        item.setPosition(iItem.getPosition());
        item.setType(itemType);
        if (iItem.getItemSpecId() != null) {
            ItemSpec itemSpec = context.getItemSpecById(iItem.getItemSpecId());
            item.setSpecification(itemSpec);
        }
        return item;
    }

    protected abstract AbstractItem convert(IntItem item, ItemType itemType);

}
