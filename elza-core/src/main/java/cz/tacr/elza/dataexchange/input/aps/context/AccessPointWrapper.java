package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;

import org.apache.commons.lang3.Validate;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;

/**
 * Access point entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper, EntityMetrics {

    private ApAccessPoint entity;

    private final AccessPointInfo apInfo;

    AccessPointWrapper(ApAccessPoint entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    @Override
    public PersistType getPersistType() {
        return apInfo.getPersistType();
    }

    /**
     * Updates wrapper with existing AP. When existing AP is older then importing
     * entity no operation is needed.
     *
     * @throws DEImportException
     *             When scopes or types does not match.
     */
    public void prepareUpdate(ApAccessPointInfo info) {
        if (!entity.getScopeId().equals(info.getScopeId())) {
            throw new DEImportException("Scope of importing AP doesn't match with scope of existing AP, import scopeId:"
                    + entity.getScopeId() + ", existing scopeId:" + info.getScopeId());
        }
        if (!entity.getApTypeId().equals(info.getApTypeId())) {
            throw new DEImportException("Type of importing AP doesn't match with type of existing AP, import typeId:"
                    + entity.getApTypeId() + ", existing typeId:" + info.getApTypeId());
        }
        // change current to existing AP
        entity = info;
        apInfo.setPersistType(PersistType.UPDATE);
        afterEntityPersist();
        // TODO: implement how to detect older AP and which versionable sub-entity
        // should be updated.
    }

    @Override
    public ApAccessPoint getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void afterEntityPersist() {
        apInfo.setEntityId(entity.getAccessPointId());
        apInfo.onEntityPersist(getMemoryScore());
    }
}
