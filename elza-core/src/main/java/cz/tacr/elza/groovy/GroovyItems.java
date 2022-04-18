package cz.tacr.elza.groovy;

import javax.validation.constraints.NotNull;
import java.util.*;

public class GroovyItems {

    private List<GroovyItem> allItems = new ArrayList<>();

    private Map<String, List<GroovyItem>> mapItems = new HashMap<>();

    public List<GroovyItem> getAllItems() {
        return allItems;
    }

    public Map<String, List<GroovyItem>> getItems() {
        return mapItems;
    }

    public List<GroovyItem> getItems(@NotNull String itemType) {
        return mapItems.getOrDefault(itemType, Collections.emptyList());
    }

    public void addItem(final GroovyItem item) {
        addItem(item.getTypeCode(), item);
    }

    public void addItem(@NotNull final String itemType, final GroovyItem item) {
        List<GroovyItem> groovyItems = mapItems.computeIfAbsent(itemType, k -> new ArrayList<>());
        groovyItems.add(item);
        allItems.add(item);
    }

}
