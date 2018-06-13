package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;

public class ArrStructItemWrapper implements EntityWrapper {

    private final ArrStructuredItem entity;

    private final EntityIdHolder<ArrStructuredObject> structObjectIdHolder;

    private EntityIdHolder<ArrData> dataIdHolder;

    ArrStructItemWrapper(ArrStructuredItem entity, EntityIdHolder<ArrStructuredObject> structObjectIdHolder) {
        this.entity = Validate.notNull(entity);
        this.structObjectIdHolder = Validate.notNull(structObjectIdHolder);
    }

    void setDataIdHolder(EntityIdHolder<ArrData> dataIdHolder) {
        this.dataIdHolder = dataIdHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
    }

    @Override
    public ArrStructuredItem getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getStructuredObject() == null);
        entity.setStructuredObject(structObjectIdHolder.getEntityRef(session));
        // set data reference if exist
        Validate.isTrue(entity.isUndefined());
        if (dataIdHolder != null) {
            entity.setData(dataIdHolder.getEntityRef(session));
        }
    }
}
