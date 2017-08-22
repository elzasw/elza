package cz.tacr.elza.deimport.storage;

import org.hibernate.Session;

/**
 * Entity wrapper for import storage process.
 */
public interface EntityWrapper {

    boolean isCreate();

    boolean isUpdate();

    Object getEntity();

    void beforeEntityPersist(Session session);

    /**
     * Default implementation is empty.
     */
    default void afterEntityPersist() {
    }
}
