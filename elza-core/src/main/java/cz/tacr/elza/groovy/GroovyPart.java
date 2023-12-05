package cz.tacr.elza.groovy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import cz.tacr.elza.domain.RulPartType;

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
    private RulPartType partType;

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
                      final RulPartType rulPartType,
                      final GroovyItems items,
                      final List<GroovyPart> children) {
        this.aeType = aeType;
        this.preferred = preferred;
        this.partType = rulPartType;
        this.items = items;
        this.children = children;
    }

    public String getAeType() {
        return aeType;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public RulPartType getPartType() {
        return partType;
    }

    public String getPartTypeCode() {
        return partType.getCode();
    }

    public List<GroovyItem> getItems(@NotNull String itemType) {
        return items.getItems(itemType);
    }

    public List<GroovyItem> getItems() {
        return items.getAllItems();
    }

    public List<GroovyPart> getChildren() {
        return children;
    }

    public enum PreferredFilter {
        YES, NO, ALL
    }
}
