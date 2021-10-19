package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ArrStructuredObject;

public class ArrStructObjectWrapper implements EntityWrapper {

    private final SimpleIdHolder<ArrStructuredObject> idHolder = new SimpleIdHolder<>(ArrStructuredObject.class);

    private final ArrStructuredObject entity;

    private final String importId;

    /**
     * Recommended SO uuid
     */
    private String uuid;

    ArrStructObjectWrapper(final ArrStructuredObject entity,
                           final String importId,
                           final String uuid) {
        this.entity = Validate.notNull(entity);
        this.importId = Validate.notNull(importId);
        this.uuid = uuid;
    }

    public String getImportId() {
        return importId;
    }

    public String getUuid() {
        return uuid;
    }

    public EntityIdHolder<ArrStructuredObject> getIdHolder() {
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
        idHolder.setEntityId(entity.getStructuredObjectId());
    }
}
