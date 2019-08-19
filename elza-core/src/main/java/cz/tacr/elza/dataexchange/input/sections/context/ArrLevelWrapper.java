package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;

public class ArrLevelWrapper implements EntityWrapper {

    private final SimpleIdHolder<ArrLevel> idHolder = new SimpleIdHolder<>(ArrLevel.class);

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
    public Object getEntity() {
        return entity;
    }

    @Override
    public SaveMethod getSaveMethod() {
        return SaveMethod.CREATE;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare node reference
        Validate.isTrue(entity.getNode() == null);
        entity.setNode(nodeIdHolder.getEntityRef(session));
        // prepare parent reference (null for root level)
        Validate.isTrue(entity.getNodeParent() == null);
        if (parentNodeIdHolder != null) {
            entity.setNodeParent(parentNodeIdHolder.getEntityRef(session));
        }
    }

    @Override
    public void afterEntitySave(Session session) {
        // init id holder
        idHolder.setEntityId(entity.getLevelId());
    }
}
