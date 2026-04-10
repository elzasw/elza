package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrNode;

public class ArrNodeWrapper implements EntityWrapper {

    private final SimpleIdHolder<ArrNode> idHolder = new SimpleIdHolder<>(ArrNode.class);

    protected final ArrNode entity;

    public ArrNodeWrapper(ArrNode entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    public EntityIdHolder<ArrNode> getIdHolder() {
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
        // NOP   
    }

    @Override
    public void afterEntitySave(Session session) {
        idHolder.setEntityId(entity.getNodeId());
    }
}
