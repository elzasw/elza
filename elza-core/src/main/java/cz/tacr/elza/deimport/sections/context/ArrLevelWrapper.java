package cz.tacr.elza.deimport.sections.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.SimpleIdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;

public class ArrLevelWrapper implements EntityWrapper {

    private final SimpleIdHolder idHolder = new SimpleIdHolder(ArrLevel.class);

    private final ArrLevel entity;

    private final IdHolder nodeIdHolder;

    private final IdHolder parentNodeIdHolder;

    ArrLevelWrapper(ArrLevel entity, IdHolder nodeIdHolder, IdHolder parentNodeIdHolder) {
        this.entity = Objects.requireNonNull(entity);
        this.nodeIdHolder = Objects.requireNonNull(nodeIdHolder);
        this.parentNodeIdHolder = parentNodeIdHolder;
    }

    public IdHolder getIdHolder() {
        return idHolder;
    }

    @Override
    public EntityState getState() {
        return EntityState.CREATE;
    }

    @Override
    public ArrLevel getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Assert.isNull(entity.getNode());
        entity.setNode(nodeIdHolder.getEntityRef(session, ArrNode.class));

        // sets parent reference (null for root level)
        Assert.isNull(entity.getNodeParent());
        if (parentNodeIdHolder != null) {
            entity.setNodeParent(parentNodeIdHolder.getEntityRef(session, ArrNode.class));
        }
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getLevelId());
    }
}
