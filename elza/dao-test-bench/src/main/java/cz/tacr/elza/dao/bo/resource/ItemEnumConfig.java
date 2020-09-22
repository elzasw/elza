package cz.tacr.elza.dao.bo.resource;

import cz.tacr.elza.ws.types.v1.ItemEnum;

public class ItemEnumConfig extends ItemConfig {

    @Override
    public ItemEnum getItem() {
        ItemEnum item = new ItemEnum();
        item.setType(getItemType());
        item.setSpec(getItemSpec());
        item.setReadOnly(getReadOnly());
        return item;
    }

}
