package cz.tacr.elza.groovy;

import cz.tacr.elza.api.IUnitdate;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;

public class GroovyItems {

    private Map<String, List<GroovyItem>> items = new HashMap<>();

    public void addItem(@NotNull final String itemType, @Nullable final String spec, @Nullable final String specCode, @NotNull final Boolean value) {
        addItem(itemType, new GroovyItem(itemType, spec, specCode, value));
    }

    public void addItem(@NotNull final String itemType, @Nullable final String spec, @Nullable final String specCode, @NotNull final Integer value) {
        addItem(itemType, new GroovyItem(itemType, spec, specCode, value));
    }

    public void addItem(@NotNull final String itemType, @Nullable final String spec, @Nullable final String specCode, @NotNull final String value) {
        addItem(itemType, new GroovyItem(itemType, spec, specCode, value));
    }

    public void addItem(@NotNull final String itemType, @Nullable final String spec, @Nullable final String specCode, @NotNull final IUnitdate value) {
        addItem(itemType, new GroovyItem(itemType, spec, specCode, value));
    }

    public List<GroovyItem> getItems(@NotNull String itemType) {
        return items.getOrDefault(itemType.toLowerCase(), Collections.emptyList());
    }

    public void addItem(@NotNull final String itemType, final GroovyItem item) {
        List<GroovyItem> groovyItems = items.computeIfAbsent(itemType.toLowerCase(), k -> new ArrayList<>());
        groovyItems.add(item);
    }

}
