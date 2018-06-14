package cz.tacr.elza.dataexchange.input.storage;

import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;

/**
 * Entity wrapper for import storage process.
 */
public interface EntityWrapper {

    PersistMethod getPersistMethod();

    Object getEntity();

    void beforeEntityPersist(Session session);

    /**
     * Default implementation is empty.
     */
    default void afterEntityPersist() {
    }

    default void evictFrom(Session session) {
        session.evict(getEntity());
    }
}
