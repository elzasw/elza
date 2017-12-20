package cz.tacr.elza.dataexchange.input.context;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.common.db.HibernateUtils;

public class EntityIdHolder<E> {

    private final Class<? extends E> entityClass;

    private Serializable entityId;

    public EntityIdHolder(Class<? extends E> entityClass) {
        this.entityClass = Validate.notNull(entityClass);
    }

    public Class<? extends E> getEntityClass() {
        return entityClass;
    }

    public boolean isInitialized() {
        return entityId != null;
    }

    public Serializable getEntityId() {
        return entityId;
    }

    public void setEntityId(Serializable entityId) {
        Validate.isTrue(!isInitialized(), "Entity id holder already initialized");
        Validate.notNull(entityId);

        this.entityId = entityId;
    }

    /**
     * Returns entity reference based on holder entity class and id.
     *
     * @see HibernateUtils#getEntityReference(Serializable, Class, Session, boolean)
     * @see #isReferenceInitOnDemand()
     */
    public final E getEntityReference(Session session) {
        Validate.isTrue(isInitialized(), "Entity id holder not initialized");

        return HibernateUtils.getEntityReference(entityId, entityClass, session, isReferenceInitOnDemand());
    }

    /**
     * Determines if references created by this holder can be initialized on demand.
     */
    protected boolean isReferenceInitOnDemand() {
        return false;
    }
}
