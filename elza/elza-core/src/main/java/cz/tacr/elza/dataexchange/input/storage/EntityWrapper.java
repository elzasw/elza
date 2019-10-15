package cz.tacr.elza.dataexchange.input.storage;

import org.hibernate.Session;

/**
 * Entity wrapper for storage. Wrapped entity must be new or loaded entity.
 */
public interface EntityWrapper {

    /**
     * Wrapped entity.
     */
    Object getEntity();

    /**
     * Save method of wrapped entity.
     */
    SaveMethod getSaveMethod();

    /**
     * Heuristic value which determines memory footprint of wrapper. Default
     * value is 1.
     */
    default long getMemoryScore() {
        return 1;
    }

    /**
     * Evicts all entities initialized by wrapper from persistent context. Default
     * implementation evicts only wrapped entity.
     */
    default void evictFrom(Session session) {
        session.evict(getEntity());
    }

    default void persist(Session session) {
        session.persist(getEntity());
    }

    default void merge(Session session) {
        session.merge(getEntity());
    }

    /**
     * Callback is used by storage to notify wrapper before entity saves.
     */
    void beforeEntitySave(Session session);

    /**
     * Callback is used by storage to notify wrapper after entity saves.
     */
    void afterEntitySave(Session session);
}
