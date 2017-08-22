package cz.tacr.elza.deimport.sections.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.context.SimpleIdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;

public class ArrDescItemWrapper implements EntityWrapper {

    private final SimpleIdHolder idHolder = new SimpleIdHolder(ArrDescItem.class);

    private final ArrDescItem entity;

    private final IdHolder nodeIdHolder;

    ArrDescItemWrapper(ArrDescItem entity, IdHolder nodeIdHolder) {
        this.entity = Objects.requireNonNull(entity);
        this.nodeIdHolder = Objects.requireNonNull(nodeIdHolder);
    }

    public IdHolder getIdHolder() {
        return idHolder;
    }

    @Override
    public boolean isCreate() {
        return true;
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public ArrDescItem getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Assert.isNull(entity.getNode());
        entity.setNode(nodeIdHolder.getEntityRef(session, ArrNode.class));
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getItemId());
    }
}
