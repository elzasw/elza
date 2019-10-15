package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrData;

public class ArrDataWrapper implements EntityWrapper {

    protected final SimpleIdHolder<ArrData> idHolder = new SimpleIdHolder<>(ArrData.class);

    protected final ArrData entity;

    ArrDataWrapper(ArrData entity) {
        this.entity = Validate.notNull(entity);
    }

    public EntityIdHolder<ArrData> getIdHolder() {
        return idHolder;
    }

    @Override
    public ArrData getEntity() {
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
        // init id holder
        idHolder.setEntityId(entity.getDataId());
    }
}
