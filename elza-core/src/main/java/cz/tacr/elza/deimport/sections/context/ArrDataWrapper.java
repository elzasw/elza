package cz.tacr.elza.deimport.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.context.SimpleIdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrData;

public class ArrDataWrapper implements EntityWrapper {

    private final SimpleIdHolder idHolder = new SimpleIdHolder(ArrData.class);

    private final ArrData entity;

    ArrDataWrapper(ArrData entity) {
        this.entity = Validate.notNull(entity);
    }

    public IdHolder getIdHolder() {
        return idHolder;
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
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getDataId());
    }
}
