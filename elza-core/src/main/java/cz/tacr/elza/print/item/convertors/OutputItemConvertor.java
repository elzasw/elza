package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.IntItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.Item;

import java.util.Arrays;
import java.util.List;

public class OutputItemConvertor implements ItemConvertor {

    public Item convert(IntItem iItem, ItemConvertorContext context) {
        if (iItem.isUndefined()) {
            return null;
        }
        for (ItemConvertor conv : getConvertors()) {
            Item item = conv.convert(iItem, context);
            if (item != null) {
                return item;
            }
        }
        throw new SystemException("Failed to convert output item", BaseCode.SYSTEM_ERROR)
                .set("itemClass", iItem.getClass())
                .set("itemId", iItem.getItemId())
                .set("itemTypeId", iItem.getItemTypeId());
    }

    private List<ItemConvertor> getConvertors() {
        return Arrays.asList(
                             new StringItemConvertor(),
                             new IntegerItemConvertor(),
                             new DecimalItemConvertor(),
                             new UnitDateItemConvertor(),
                             new CoordinatesItemConvertor(),
                             new EnumItemConvertor(),
                             new RecordRefItemConvertor(),
                             new StructuredObjectRefItemConvertor(),
                             new FileRefItemConvertor(),
                             new JsonTableItemConvertor(),
                             new DateItemConvertor());
    }
}
