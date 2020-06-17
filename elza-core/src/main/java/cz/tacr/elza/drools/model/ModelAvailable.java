package cz.tacr.elza.drools.model;


import cz.tacr.elza.drools.model.item.AbstractItem;
import java.util.List;

public class ModelAvailable {

    private Ap ap;
    private Part part;
    private List<AbstractItem> items;
    private List<ItemType> itemTypes;

    public ModelAvailable(final Ap ap, final Part part, final List<AbstractItem> items, final List<ItemType> itemTypes) {
        this.ap = ap;
        this.part = part;
        this.items = items;
        this.itemTypes = itemTypes;
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
}
