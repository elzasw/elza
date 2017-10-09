package cz.tacr.elza.deimport.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.context.SimpleIdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;

public class ArrDescItemWrapper implements EntityWrapper {

    private final SimpleIdHolder idHolder = new SimpleIdHolder(ArrDescItem.class);

    private final ArrDescItem entity;

    private final IdHolder nodeIdHolder;

    private IdHolder dataIdHolder;

    ArrDescItemWrapper(ArrDescItem entity, IdHolder nodeIdHolder) {
        this.entity = Validate.notNull(entity);
        this.nodeIdHolder = Validate.notNull(nodeIdHolder);
    }

    void setDataIdHolder(IdHolder dataIdHolder) {
        this.dataIdHolder = dataIdHolder;
    }

    @Override
    public EntityState getState() {
        return EntityState.CREATE;
    }

    @Override
    public ArrDescItem getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getNode() == null);
        entity.setNode(nodeIdHolder.getEntityRef(session, ArrNode.class));
        // set data reference if exist
        Validate.isTrue(entity.isUndefined());
        if (dataIdHolder != null) {
            entity.setData(dataIdHolder.getEntityRef(session, ArrData.class));
        }
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getItemId());
    }
}
