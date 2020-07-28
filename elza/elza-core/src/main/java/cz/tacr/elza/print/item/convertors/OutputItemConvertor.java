package cz.tacr.elza.print.item.convertors;

import java.util.Arrays;
import java.util.List;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.Item;

public class OutputItemConvertor {

    final ItemConvertorContext context;

    final List<ItemConvertor> convertors;

    public OutputItemConvertor(final ItemConvertorContext context) {
        this.context = context;

        this.convertors = Arrays
                .asList(
                                   new StringItemConvertor(),
                                   new IntegerItemConvertor(),
                                   new DecimalItemConvertor(),
                                   new UnitDateItemConvertor(),
                                   new CoordinatesItemConvertor(),
                                   new EnumItemConvertor(),
                                   new RecordRefItemConvertor(),
                                   new PartyRefItemConvertor(),
                                   new StructuredObjectRefItemConvertor(),
                                   new FileRefItemConvertor(),
                                   new JsonTableItemConvertor(),
                                   new DateItemConvertor(),
                                   new UriRefItemConvertor());
    }

    public Item convert(ArrItem arrItem) {
        if (arrItem.isUndefined()) {
            return null;
        }
        for (ItemConvertor conv : convertors) {
            Item item = conv.convert(arrItem, context);
            if (item != null) {
                return item;
            }
        }
        throw new SystemException("Failed to convert output item", BaseCode.SYSTEM_ERROR)
                .set("arrItemId", arrItem.getItemId())
                .set("itemTypeId", arrItem.getItemTypeId());
    }

}
