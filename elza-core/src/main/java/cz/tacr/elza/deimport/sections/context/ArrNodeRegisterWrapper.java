package cz.tacr.elza.deimport.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;

public class ArrNodeRegisterWrapper implements EntityWrapper {

    private final ArrNodeRegister entity;

    private final IdHolder nodeIdHolder;

    ArrNodeRegisterWrapper(ArrNodeRegister entity, IdHolder nodeIdHolder) {
        this.entity = Validate.notNull(entity);
        this.nodeIdHolder = Validate.notNull(nodeIdHolder);
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
        Validate.isTrue(entity.getNode() == null);
        entity.setNode(nodeIdHolder.getEntityRef(session, ArrNode.class));
    }
}
