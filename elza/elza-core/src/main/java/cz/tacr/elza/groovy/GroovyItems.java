package cz.tacr.elza.groovy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import cz.tacr.elza.core.data.ItemType;

/**
 * Collection of GroovyItems
 * 
 * Items might be returned by item type.
 */
public class GroovyItems {

    private final List<GroovyItem> allItems = new ArrayList<>();

    private final Map<ItemType, List<GroovyItem>> mapItems = new HashMap<>();

    public List<GroovyItem> getAllItems() {
        return allItems;
    }

    public List<GroovyItem> getItems(@NotNull ItemType itemType) {
        return mapItems.getOrDefault(itemType, Collections.emptyList());
    }

    public void addItem(@NotNull final GroovyItem item) {
        List<GroovyItem> groovyItems = mapItems.computeIfAbsent(item.getItemType(), k -> new ArrayList<>());
        groovyItems.add(item);
        allItems.add(item);
    }

}
