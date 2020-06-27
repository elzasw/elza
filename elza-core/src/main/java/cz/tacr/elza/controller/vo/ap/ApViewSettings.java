package cz.tacr.elza.controller.vo.ap;

import cz.tacr.elza.packageimport.xml.SettingItemTypes;
import cz.tacr.elza.packageimport.xml.SettingPartsOrder;

import java.util.List;

public class ApViewSettings {

    private List<SettingPartsOrder.Part> partsOrder;
    private List<SettingItemTypes.ItemType> itemTypes;

    public List<SettingPartsOrder.Part> getPartsOrder() {
        return partsOrder;
    }

    public void setPartsOrder(final List<SettingPartsOrder.Part> partsOrder) {
        this.partsOrder = partsOrder;
    }

    public List<SettingItemTypes.ItemType> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(final List<SettingItemTypes.ItemType> itemTypes) {
        this.itemTypes = itemTypes;
    }
}
