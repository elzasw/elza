package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApName;

/**
 * Access point name wrapper.
 */
public class ApNameWrapper implements EntityWrapper {

    private final ApName entity;

    private final AccessPointInfo apInfo;

    ApNameWrapper(ApName entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    @Override
    public SaveMethod getSaveMethod() {
        SaveMethod sm = apInfo.getSaveMethod();
        // AP name is never updated and old must be invalidate by storage
        return sm.equals(SaveMethod.IGNORE) ? sm : SaveMethod.CREATE;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare AP reference
        Validate.isTrue(entity.getAccessPoint() == null);
        entity.setAccessPoint(apInfo.getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
        // update AP info
        apInfo.onEntityPersist();
    }
}
