package cz.tacr.elza.deimport.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;

public class ArrDataWrapper implements EntityWrapper {

    private final ArrData entity;

    private final IdHolder descItemIdHolder;

    ArrDataWrapper(ArrData entity, IdHolder descItemIdHolder) {
        this.entity = Validate.notNull(entity);
        this.descItemIdHolder = Validate.notNull(descItemIdHolder);
    }

    @Override
    public EntityState getState() {
        return EntityState.CREATE;
    }

    @Override
    public ArrData getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getItem() == null);
        entity.setItem(descItemIdHolder.getEntityRef(session, ArrDescItem.class));
    }
}
