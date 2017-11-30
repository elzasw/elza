package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrNode;

public class ArrNodeWrapper implements EntityWrapper {

    private final EntityIdHolder<ArrNode> idHolder = new EntityIdHolder<>(ArrNode.class);

    protected final ArrNode entity;

    ArrNodeWrapper(ArrNode entity) {
        this.entity = Validate.notNull(entity);
    }

    public EntityIdHolder<ArrNode> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
    }

    @Override
    public ArrNode getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getNodeId());
    }
}
