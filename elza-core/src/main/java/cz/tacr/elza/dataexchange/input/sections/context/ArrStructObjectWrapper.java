package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrStructuredObject;

public class ArrStructObjectWrapper implements EntityWrapper {

    private final EntityIdHolder<ArrStructuredObject> idHolder = new EntityIdHolder<>(ArrStructuredObject.class);

    private final ArrStructuredObject entity;

    private final String importId;

    ArrStructObjectWrapper(ArrStructuredObject entity, String importId) {
        this.entity = Validate.notNull(entity);
        this.importId = Validate.notNull(importId);
    }

    public String getImportId() {
        return importId;
    }

    public EntityIdHolder<ArrStructuredObject> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
    }

    @Override
    public ArrStructuredObject getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getStructuredObjectId());
    }
}
