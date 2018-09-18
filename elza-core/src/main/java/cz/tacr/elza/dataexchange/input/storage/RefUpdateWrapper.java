package cz.tacr.elza.dataexchange.input.storage;

import org.hibernate.Session;

/**
 * Wrapper for updating entity. This wrapper is useful for cases where entity
 * can be detached (not loaded) e.g. by storage manager.
 */
public interface RefUpdateWrapper {

    /**
     * If true update of entity reference is no longer needed.
     */
    boolean isIgnored();

    /**
     * If true entity is POJO or initialized Hibernate proxy.
     */
    boolean isLoaded(Session session);

    /**
     * Merge loaded entity to given session.
     */
    void merge(Session session);

    /**
     * Executes update query. Query is used if entity is uninitialized
     * Hibernate proxy i.e. not loaded. Query avoids any unnecessary fetches from database.
     */
    void executeUpdateQuery(Session session);
}
