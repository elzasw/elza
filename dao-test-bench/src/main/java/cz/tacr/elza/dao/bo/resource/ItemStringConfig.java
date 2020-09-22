package cz.tacr.elza.dao.bo.resource;

import cz.tacr.elza.ws.types.v1.ItemString;

public class ItemStringConfig extends ItemConfig {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public ItemString getItem() {
        ItemString item = new ItemString();
        item.setType(getItemType());
        item.setSpec(getItemSpec());
        item.setValue(value);
        item.setReadOnly(getReadOnly());
        return item;
    }

}
