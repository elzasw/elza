package cz.tacr.elza.deimport.context;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.hibernate.Session;

import cz.tacr.elza.utils.HibernateUtils;

/**
 * Id holder for the entity which needs delayed initialization and the memory footprint of the
 * reference must be kept to minimum.
 */
public abstract class IdHolder {

    private Serializable id;

    public Serializable getId() {
        return id;
    }

    public boolean isInitialized() {
        return id != null;
    }

    /**
     * Creates entity reference from id with cache mode {@link CacheMode#GET} (which is suitable
     * only for read).
     *
     * @throws IllegalStateException When holder is not initialized or class is not acceptable.
     * @see HibernateUtils#getReference(Session, Class, Serializable, boolean)
     */
    public final <E> E getEntityRef(Session session, Class<E> entityClass) {
        Validate.isTrue(isInitialized(), "Holder is not initialized");
        checkReferenceClass(entityClass);
        return HibernateUtils.getReference(session, entityClass, id, false);
    }

    /**
     * Check if class is acceptable as entity reference, throws runtime exception when not.
     */
    public abstract void checkReferenceClass(Class<?> entityClass);

    protected void init(Serializable id) {
        Validate.isTrue(!isInitialized(), "Holder is already initialized");
        Validate.notNull(id);
        this.id = id;
    }
}
