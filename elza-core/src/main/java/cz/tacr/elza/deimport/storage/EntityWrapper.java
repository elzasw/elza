package cz.tacr.elza.deimport.storage;

import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;

/**
 * Entity wrapper for import storage process.
 */
public interface EntityWrapper {

    EntityState getState();

    Object getEntity();

    void beforeEntityPersist(Session session);

    /**
     * Default implementation is empty.
     */
    default void afterEntityPersist() {
    }
}
