package cz.tacr.elza.drools.model;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.drools.model.item.AbstractItem;

public class ModelAvailable {

    final private Ap ap;
    final private Part part;
    final private List<AbstractItem> items;
    final private List<ItemType> itemTypes;
    final private Map<String, ItemType> mapItemTypes;

    public ModelAvailable(final Ap ap, final Part part, final List<AbstractItem> items, final List<ItemType> itemTypes) {
        this.ap = ap;
        this.part = part;
        this.items = items;
        this.itemTypes = itemTypes;

        mapItemTypes = itemTypes.stream().collect(Collectors.toMap(it -> it.getItemType().getCode(), Function
                .identity()));
    }

    public Ap getAp() {
        return ap;
    }

    public Part getPart() {
        return part;
    }

    public List<AbstractItem> getItems() {
        return items;
    }

    public List<ItemType> getItemTypes() {
        return itemTypes;
    }

    public ItemType getItemType(String itemTypeCode) {
        return mapItemTypes.get(itemTypeCode);
    }

    public ItemType getItemType(AbstractItem item) {
        return mapItemTypes.get(item.getItemType().getCode());
    }

    public AbstractItem findItem(cz.tacr.elza.core.data.ItemType itemType) {
        for (AbstractItem item : items) {
            if (item.getItemType() == itemType) {
                return item;
            }
        }
        return null;
    }

    public AbstractItem findItem(String itemTypeCode) {
        for (AbstractItem item : items) {
            if (item.getItemType().getCode().equals(itemTypeCode)) {
                return item;
            }
        }
        return null;
    }
}
