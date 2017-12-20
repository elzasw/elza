package cz.tacr.elza.common.db;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.hcore.util.impl.HibernateHelper;

/**
 * Helper class for Hibernate.
 */
public class HibernateUtils {

    /**
     * Unproxy specified object. In case of uninitialized proxy will be entity loaded from database.
     *
     * @param object POJO or hibernate proxy
     *
     * @return POJO or initialized entity, can be null when object was null.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unproxy(Object object) {
        if (object == null) {
            return null;
        }
        return (T) HibernateHelper.unproxy(object);
    }

    /**
     * Unproxy active session from entity manager for current thread.
     */
    public static Session getCurrentSession(EntityManager em) {
        return em.unwrap(Session.class);
    }

    /**
     * Returns currently running transaction for specified session.
     */
    public static Transaction getCurrentTransaction(EntityManager em) {
        SharedSessionContractImplementor ssci = em.unwrap(SharedSessionContractImplementor.class);
        Validate.isTrue(ssci.isTransactionInProgress());
        Transaction tx = ssci.accessTransaction();
        Validate.isTrue(tx.isActive());
        return tx;
    }

    /**
     * Test if specified object is initialized.
     *
     * @param object pojo or hibernate proxy
     * @return True when specified object is not proxy or LazyInitializer is initialized.
     */
    public static boolean isInitialized(Object object) {
        if (object == null) {
            return false;
        }
        return Hibernate.isInitialized(object);
    }

    /**
     * Get entity from persistent context or create proxy if missing. Exception is thrown when
     * entity does not exist during proxy initialization.
     *
     * @param entityId not-null
     * @param entityClass not-null
     * @param session active session
     * @param activeProxy Active proxy can be initialized on demand or used for updates.
     *
     * @throws RuntimeException When holder id is not set or entity class is not acceptable.
     */
    public static <E> E getEntityReference(Serializable entityId, Class<E> entityClass, Session session, boolean activeProxy) {
        // L2 cache access
        CacheMode cacheMode = activeProxy ? CacheMode.NORMAL : CacheMode.GET;

        // retrieve entity or create proxy
        E entity = session.byId(entityClass).with(cacheMode).getReference(entityId);

        // L1 cache access
        if (!activeProxy && !isInitialized(entity)) {
            session.evict(entity);
        }
        return entity;
    }
}
