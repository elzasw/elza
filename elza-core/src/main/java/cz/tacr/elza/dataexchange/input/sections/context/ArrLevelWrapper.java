package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;

public class ArrLevelWrapper implements EntityWrapper {

    private final EntityIdHolder<ArrLevel> idHolder = new EntityIdHolder<>(ArrLevel.class);

    private final ArrLevel entity;

    private final EntityIdHolder<ArrNode> nodeIdHolder;

    private final EntityIdHolder<ArrNode> parentNodeIdHolder;

    ArrLevelWrapper(ArrLevel entity, EntityIdHolder<ArrNode> nodeIdHolder, EntityIdHolder<ArrNode> parentNodeIdHolder) {
        this.entity = Validate.notNull(entity);
        this.nodeIdHolder = Validate.notNull(nodeIdHolder);
        this.parentNodeIdHolder = parentNodeIdHolder;
    }

    public EntityIdHolder<ArrLevel> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
    }

    @Override
    public ArrLevel getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getNode() == null);
        entity.setNode(nodeIdHolder.getEntityReference(session));

        // sets parent reference (null for root level)
        Validate.isTrue(entity.getNodeParent() == null);
        if (parentNodeIdHolder != null) {
            entity.setNodeParent(parentNodeIdHolder.getEntityReference(session));
        }
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getLevelId());
    }
}
