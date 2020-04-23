package cz.tacr.elza.dataexchange.input.parts.context;

import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

public class ItemWrapper implements EntityWrapper {

    private final SimpleIdHolder<ApPart> partIdHolder = new SimpleIdHolder<>(ApPart.class, false);

    private final ApItem entity;

    private final PartInfo partInfo;

    public ItemWrapper(ApItem entity, PartInfo partInfo) {
        this.entity = entity;
        this.partInfo = partInfo;
    }

    public SimpleIdHolder<ApPart> getPartIdHolder() {
        return partIdHolder;
    }


    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public SaveMethod getSaveMethod() {
        SaveMethod sm = partInfo.getSaveMethod();
        // party name is never updated and old must be invalidate by storage
        return sm.equals(SaveMethod.IGNORE) ? sm : SaveMethod.CREATE;
    }

    @Override
    public void beforeEntitySave(Session session) {
        Validate.isTrue(entity.getPartId() == null);
        entity.setPart(partInfo.getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
        partInfo.onEntityPersist();
    }
}
