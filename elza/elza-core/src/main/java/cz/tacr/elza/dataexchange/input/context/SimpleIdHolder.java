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
public class SimpleIdHolder<E> implements EntityIdHolder<E> {

    private final Class<? extends E> entityClass;

    private final boolean detachIfNotLoaded;

    private Integer entityId;

    public SimpleIdHolder(Class<? extends E> entityClass, boolean detachIfNotLoaded) {
        this.entityClass = Validate.notNull(entityClass);
        this.detachIfNotLoaded = detachIfNotLoaded;
    }

    public SimpleIdHolder(Class<? extends E> entityClass) {
        this(entityClass, true);
    }

    public boolean hasEntityId() {
        return entityId != null;
    }

    @Override
    public Integer getEntityId() {
        Validate.notNull(entityId);

        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public final E getEntityRef(Session session) {
        Validate.notNull(entityId);

        return HibernateUtils.getEntityRef(entityId, entityClass, session, detachIfNotLoaded);
    }
}
