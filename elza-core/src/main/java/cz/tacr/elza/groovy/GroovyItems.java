package cz.tacr.elza.groovy;

import javax.validation.constraints.NotNull;
import java.util.*;

public class GroovyItems {

    private Map<String, List<GroovyItem>> items = new HashMap<>();

    public Map<String, List<GroovyItem>> getItems() {
        return items;
    }

    public List<GroovyItem> getItems(@NotNull String itemType) {
        return items.getOrDefault(itemType.toLowerCase(), Collections.emptyList());
    }

    public void addItem(@NotNull final String itemType, final GroovyItem item) {
        List<GroovyItem> groovyItems = items.computeIfAbsent(itemType.toLowerCase(), k -> new ArrayList<>());
        groovyItems.add(item);
    }

    public void addItem(final GroovyItem item) {
        addItem(item.getTypeCode(), item);
    }

}
