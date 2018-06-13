package cz.tacr.elza.dataexchange.input.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.common.db.HibernateUtils;

/**
 * Class is used as ID transfer object.
 * 
 * Class allows to assign ID to object later, typically when object is stored in
 * DB.
 * 
 * @param <E>
 */
public class EntityIdHolder<E> {

    private final Class<? extends E> entityClass;

    private final boolean detachProxyIfNotLoaded;

    private Integer entityId;

    public EntityIdHolder(Class<? extends E> entityClass, boolean detachProxyIfNotLoaded) {
        this.entityClass = Validate.notNull(entityClass);
        this.detachProxyIfNotLoaded = detachProxyIfNotLoaded;
    }

    public EntityIdHolder(Class<? extends E> entityClass) {
        this(entityClass, true);
    }

    public Class<? extends E> getEntityClass() {
        return entityClass;
    }

    /**
     * Check if entity has already ID
     * 
     * @return
     */
    public boolean hasEntityId() {
        return entityId != null;
    }

    /**
     * Return entity ID
     * 
     * @return
     */
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        Validate.isTrue(!hasEntityId());
        Validate.notNull(entityId);

        this.entityId = entityId;
    }

    /**
     * Return the persistent instance with the given identifier, assuming that the
     * instance exists. This method might return a proxied instance.
     */
    public final E getEntityRef(Session session) {
        Validate.isTrue(hasEntityId());

        return HibernateUtils.getEntityRef(entityId, entityClass, session, detachProxyIfNotLoaded);
    }
}
