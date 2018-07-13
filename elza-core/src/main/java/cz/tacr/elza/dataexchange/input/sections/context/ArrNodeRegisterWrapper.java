package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;

public class ArrNodeRegisterWrapper implements EntityWrapper {

    private final ArrNodeRegister entity;

    private final EntityIdHolder<ArrNode> nodeIdHolder;

    ArrNodeRegisterWrapper(ArrNodeRegister entity, EntityIdHolder<ArrNode> nodeIdHolder) {
        this.entity = Validate.notNull(entity);
        this.nodeIdHolder = Validate.notNull(nodeIdHolder);
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
    }
}
