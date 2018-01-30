package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrStructureData;

public class ArrStructObjectWrapper implements EntityWrapper {

    private final EntityIdHolder<ArrStructureData> idHolder = new EntityIdHolder<>(ArrStructureData.class);

    private final ArrStructureData entity;

    private final String importId;

    ArrStructObjectWrapper(ArrStructureData entity, String importId) {
        this.entity = Validate.notNull(entity);
        this.importId = Validate.notNull(importId);
    }

    public String getImportId() {
        return importId;
    }

    public EntityIdHolder<ArrStructureData> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
    }

    @Override
    public ArrStructureData getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getStructureDataId());
    }
}
