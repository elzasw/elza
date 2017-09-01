package cz.tacr.elza.utils;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.util.Assert;

/**
 * Pomocná třída pro práci s objekty s HibernateProxy.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 28. 4. 2016
 */
public class HibernateUtils {

    /**
     * Unwrap proxied entity. When object is instance of HibernateProxy then initialization is
     * performed.
     *
     * @param object POJO or hibernate proxy, can be null
     *
     * @return object POJO casted to required type
     */
    @SuppressWarnings("unchecked")
    public static <T> T unproxy(Object object) {
        LazyInitializer initializer = getLazyInitializer(object);
        if (initializer != null) {
            return (T) initializer.getImplementation();
        }
        return (T) object;
    }

    /**
     * Get the actual class of proxied entity.
     *
     * @param object POJO or hibernate proxy, not-null
     */
    public static Class<?> getPersistentClass(Object object) {
        Assert.notNull(object);

        LazyInitializer initializer = getLazyInitializer(object);
        if (initializer != null) {
            return initializer.getPersistentClass();
        }
        return object.getClass();
    }

    /**
     * Gets LazyInitializer from HibernateProxy.
     *
     * @return Lazy initializer or null when object is not instance of HibernateProxy.
     */
    public static LazyInitializer getLazyInitializer(Object object) {
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
     * Get entity from persistent context or create proxy for expected entity (cause error during
     * initialization when not found).
     *
     * @param session open session
     * @param entityClass not-null
     * @param id not-null
     * @param createCacheEntry When false session may read entity from the cache, but will not add
     * entity, except to invalidate entity when updates occur.
     *
     * @see CacheMode#NORMAL
     * @see CacheMode#GET
     */
    public static <T> T getReference(Session session, Class<T> entityClass, Serializable id, boolean createCacheEntry) {
        CacheMode cm = createCacheEntry ? CacheMode.NORMAL : CacheMode.GET;
        T proxy = session.byId(entityClass).with(cm).getReference(id);
        return proxy;
    }
}
