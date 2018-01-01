package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;

public abstract class AbstractItemConvertor implements ItemConvertor {

    protected ItemConvertorContext context;

    @Override
    public final Item convert(ArrItem arrItem, ItemConvertorContext context) {
        this.context = context;

        ItemType itemType = context.getItemTypeById(arrItem.getItemTypeId());

        AbstractItem item = convert(arrItem, itemType);
        if (item == null) {
            return item;
        }
        item.setPosition(arrItem.getPosition());
        item.setType(itemType);

        if (arrItem.getItemSpecId() != null) {
            ItemSpec itemSpec = context.getItemSpecById(arrItem.getItemSpecId());
            item.setSpecification(itemSpec);
        }
        return item;
    }

    protected abstract AbstractItem convert(ArrItem item, ItemType itemType);

}
