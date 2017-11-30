package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrData;

public class ArrDataWrapper implements EntityWrapper {

    private final EntityIdHolder<ArrData> idHolder = new EntityIdHolder<>(ArrData.class);

    private final ArrData entity;

    ArrDataWrapper(ArrData entity) {
        this.entity = Validate.notNull(entity);
    }

    public EntityIdHolder<ArrData> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
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
        idHolder.setEntityId(entity.getDataId());
    }
}
