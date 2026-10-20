package cz.tacr.elza.groovy;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class GroovyResult {

    public static final String PT_PREFER_NAME = "PT_PREFER_NAME";
    public static final String DISPLAY_NAME = "DISPLAY_NAME";
    public static final String DISPLAY_NAME_LOWER = "DISPLAY_NAME_LOWER";
    public static final String SORT_NAME = "SORT_NAME";
    public static final String SHORT_NAME = "SHORT_NAME";
    /**
     * The name as it is commonly used: without supplements and degrees, a person in the natural
     * order of the words. The exact match of a search text, see the {@code index-type} fields of the
     * search configuration.
     */
    public static final String REAL_NAME = "REAL_NAME";

    private GroovyKeyValue keyValue;

    private Map<String, String> indexes = new HashMap<>();

    public void setKeyValue(@NotNull final String key, @NotNull final String value) {
        keyValue = new GroovyKeyValue(key, value);
    }

    public void setPtPreferName(final String value) {
        keyValue = new GroovyKeyValue(PT_PREFER_NAME, value);
    }

    public void addIndex(@NotNull final String key, @NotNull final String value) {
        indexes.put(key, value);
    }

    public void setDisplayName(@NotNull final String value) {
        indexes.put(DISPLAY_NAME, value);
        indexes.put(DISPLAY_NAME_LOWER, value.toLowerCase());
    }

    public void setSortName(@NotNull final String value) {
        indexes.put(SORT_NAME, value);
    }

    public GroovyKeyValue getKeyValue() {
        return keyValue;
    }

    public Map<String, String> getIndexes() {
        return indexes;
    }
}
