package cz.tacr.elza.groovy;

import javax.validation.constraints.NotNull;
import java.util.List;

public class GroovyPart {

    /**
     * Kód typu archivní entity.
     */
    private String aeType;

    /**
     * Jedná se o preferovanou část archivní entity.
     */
    private boolean preferred;

    /**
     * Typ části archivní entity.
     */
    private String partTypeCode;

    /**
     * Itemy části archivní entity.
     */
    private GroovyItems items;

    /**
     * Související části archivní entity.
     */
    private List<GroovyPart> children;

    public GroovyPart(final String aeType,
                      final boolean preferred,
                      final String partTypeCode,
                      final GroovyItems items,
                      final List<GroovyPart> children) {
        this.aeType = aeType;
        this.preferred = preferred;
        this.partTypeCode = partTypeCode;
        this.items = items;
        this.children = children;
    }

    public String getAeType() {
        return aeType;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public String getPartTypeCode() {
        return partTypeCode;
    }

    public List<GroovyItem> getItems(@NotNull String itemType) {
        return items.getItems(itemType);
    }

    public List<GroovyPart> getChildren() {
        return children;
    }

    public enum PreferredFilter {
        YES, NO, ALL
    }
}
