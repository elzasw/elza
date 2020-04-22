package cz.tacr.elza.ws.core.v1.items;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of items
 *
 */
public class Items {
    List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

}
