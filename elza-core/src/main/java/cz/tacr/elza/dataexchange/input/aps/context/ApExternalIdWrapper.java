package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ApExternalId;

/**
 * Access point external id wrapper.
 */
public class ApExternalIdWrapper implements EntityWrapper, EntityMetrics {

    private final ApExternalId entity;

    private final AccessPointInfo apInfo;

    ApExternalIdWrapper(ApExternalId entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    @Override
    public PersistType getPersistType() {
        PersistType pt = apInfo.getPersistType();
        // external id is never updated and old must be invalidate by storage
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.CREATE;
    }

    @Override
    public ApExternalId getEntity() {
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
