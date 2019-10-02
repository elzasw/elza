package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;

public class ArrDescItemWrapper implements EntityWrapper {

    private final ArrDescItem entity;

    private final EntityIdHolder<ArrNode> nodeIdHolder;

    private EntityIdHolder<ArrData> dataIdHolder;

    ArrDescItemWrapper(ArrDescItem entity, EntityIdHolder<ArrNode> nodeIdHolder) {
        this.entity = Validate.notNull(entity);
        this.nodeIdHolder = Validate.notNull(nodeIdHolder);
    }

    void setDataIdHolder(EntityIdHolder<ArrData> dataIdHolder) {
        this.dataIdHolder = dataIdHolder;
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
        // prepare data reference
        Validate.isTrue(entity.isUndefined());
        if (dataIdHolder != null) {
            entity.setData(dataIdHolder.getEntityRef(session));
        }
    }

    @Override
    public void afterEntitySave(Session session) {
    }
}
