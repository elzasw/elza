package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrNode;

public class ArrInhibitedItemWrapper implements EntityWrapper {

    private final ArrInhibitedItem entity;

    private final EntityIdHolder<ArrNode> nodeIdHolder;

    private final Integer descItemObjectId;

    ArrInhibitedItemWrapper(ArrInhibitedItem entity, EntityIdHolder<ArrNode> nodeIdHolder, Integer descItemObjectId) {
        this.entity = Objects.requireNonNull(entity);
        this.nodeIdHolder = Objects.requireNonNull(nodeIdHolder);
        this.descItemObjectId = Objects.requireNonNull(descItemObjectId);
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

        // set correct ObjectId
        entity.setDescItemObjectId(descItemObjectId);
    }

    @Override
    public void afterEntitySave(Session session) {
    }
}
