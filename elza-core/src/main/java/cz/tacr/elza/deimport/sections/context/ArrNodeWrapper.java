package cz.tacr.elza.deimport.sections.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.context.SimpleIdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrNode;

public class ArrNodeWrapper implements EntityWrapper {

    private final SimpleIdHolder idHolder = new SimpleIdHolder(ArrNode.class);

    protected final ArrNode entity;

    ArrNodeWrapper(ArrNode entity) {
        this.entity = Objects.requireNonNull(entity);
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
    public ArrNode getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getNodeId());
    }
}
