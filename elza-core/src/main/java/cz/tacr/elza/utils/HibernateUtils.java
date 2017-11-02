package cz.tacr.elza.utils;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Helper class for Hibernate.
 */
public class HibernateUtils {

    /**
     * Unproxy specified entity. Proxy implementor (entity) must be in initialized state.
     *
     * @param object pojo or hibernate proxy
     *
     * @return initialized entity
     */
    @SuppressWarnings("unchecked")
    public static <T> T unproxy(T object) {
        if (object == null) {
            return null;
        }
        LazyInitializer initializer = getLazyInitializer(object);
        if (initializer != null) {
            return (T) initializer.getImplementation();
        }
        return object;
    }

    /**
     * Get the actual class of proxied entity.
     *
     * @param object pojo or hibernate proxy, not-null
     */
    public static Class<?> getPersistentClass(Object object) {
        LazyInitializer initializer = getLazyInitializer(object);
        if (initializer != null) {
            return initializer.getPersistentClass();
        }
        return object.getClass();
    }

    /**
     * Get LazyInitializer from hibernate proxy.
     *
     * @param object pojo or hibernate proxy, not-null
     * @return LazyInitializer or null when object is not instance of hibernate proxy.
     */
    public static LazyInitializer getLazyInitializer(Object object) {
        Validate.notNull(object);
        if (object instanceof HibernateProxy) {
            HibernateProxy proxy = (HibernateProxy) object;
            return proxy.getHibernateLazyInitializer();
        }
        return null;
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
        LazyInitializer initializer = HibernateUtils.getLazyInitializer(object);
        return initializer == null || !initializer.isUninitialized();
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
