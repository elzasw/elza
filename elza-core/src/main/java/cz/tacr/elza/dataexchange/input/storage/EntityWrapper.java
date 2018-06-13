package cz.tacr.elza.dataexchange.input.storage;

import org.hibernate.Session;

/**
 * Entity wrapper for import storage process.
 */
public interface EntityWrapper {

    public enum PersistType {
        CREATE, UPDATE, NONE
    }

    PersistType getPersistType();

    Object getEntity();

    /**
     * Method is called before wrapped entity is persist. Default implementation is
     * empty.
     */
    default void beforeEntityPersist(Session session) {
    }

    /**
     * Method is called after wrapped entity is persist. Default implementation is
     * empty.
     */
    default void afterEntityPersist() {
    }
}
