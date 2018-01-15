package cz.tacr.elza.service.event;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Event pro invalidaci cache v jádře.
 *
 * @author Martin Šlapa
 * @since 30.11.2016
 */
public class CacheInvalidateEvent {

    /**
     * Typy cache, které se mají invalidovat.
     */
    private Set<Type> types = new HashSet<>();

    public CacheInvalidateEvent() {
        types.add(Type.ALL);
    }

    public CacheInvalidateEvent(final Type... types) {
        this.types.addAll(Arrays.asList(types));
    }

    /**
     * Typ cache pro invalidaci.
     */
    public enum Type {
        ALL,
        RULE,
        VIEW,
        GROOVY,
        LEVEL_TREE
    }

    public Set<Type> getTypes() {
        return types;
    }

    public boolean contains(final Type type) {
        return types.contains(Type.ALL) || types.contains(type);
    }
}
