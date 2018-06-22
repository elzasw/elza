package cz.tacr.elza.dataexchange.input.storage;

import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.Session;

/**
 * Wrapper for entity which may not be loaded during update.
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
     * Creates update query. Query is used if entity is uninitialized
     * Hibernate proxy. This avoids any unnecessary fetches from database.
     */
    CriteriaUpdate<?> createUpdateQuery(Session session);
}
