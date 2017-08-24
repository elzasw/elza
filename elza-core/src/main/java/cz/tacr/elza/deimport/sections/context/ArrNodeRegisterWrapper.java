package cz.tacr.elza.deimport.sections.context;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;

public class ArrNodeRegisterWrapper implements EntityWrapper {

    private final ArrNodeRegister entity;

    private final IdHolder nodeIdHolder;

    ArrNodeRegisterWrapper(ArrNodeRegister entity, IdHolder nodeIdHolder) {
        this.entity = entity;
        this.nodeIdHolder = nodeIdHolder;
    }

    @Override
    public EntityState getState() {
        return EntityState.CREATE;
    }

    @Override
    public ArrNodeRegister getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Assert.isNull(entity.getNode());
        entity.setNode(nodeIdHolder.getEntityRef(session, ArrNode.class));
    }
}
