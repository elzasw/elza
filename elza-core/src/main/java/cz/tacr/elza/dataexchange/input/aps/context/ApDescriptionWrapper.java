package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.domain.ApDescription;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;

/**
 * Access point description wrapper.
 */
public class ApDescriptionWrapper implements EntityWrapper, EntityMetrics {

    private final ApDescription entity;

    private final AccessPointInfo apInfo;

    ApDescriptionWrapper(ApDescription entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    @Override
    public PersistType getPersistType() {
        PersistType pt = apInfo.getPersistType();
        // description is never updated and old must be invalidate by storage
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.CREATE;
    }

    @Override
    public ApDescription getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getAccessPoint() == null);
        entity.setAccessPoint(apInfo.getEntityRef(session));
    }

    @Override
    public void afterEntityPersist() {
        apInfo.onEntityPersist(getMemoryScore());
    }
}
