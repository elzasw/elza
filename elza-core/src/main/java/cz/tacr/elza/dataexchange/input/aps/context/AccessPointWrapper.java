package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ApAccessPoint;

/**
 * Access point entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper, EntityMetrics {

    private ApAccessPoint entity;

    private final AccessPointInfo apInfo;

    private final MultiValuedMap<String, String> eidTypeValuedMap;

    AccessPointWrapper(ApAccessPoint entity, AccessPointInfo apInfo, MultiValuedMap<String, String> eidTypeValueMap) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
        this.eidTypeValuedMap = eidTypeValueMap;
    }

    @Override
    public PersistType getPersistType() {
        return apInfo.getPersistType();
    }

    @Override
    public ApAccessPoint getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    public MultiValuedMap<String, String> getEidTypeValueMap() {
        return eidTypeValuedMap;
    }

    /**
     * Updates wrapper with given AP. When given AP is older then importing entity
     * no operation is needed.
     *
     * @return True when given AP is newer.
     * @throws DEImportException
     *             When scopes or types does not match.
     */
    public boolean changeToUpdated(ApAccessPoint ap) {
        if (!entity.getScopeId().equals(ap.getScopeId())) {
            throw new DEImportException("Scope of importing AP doesn't match with scope of existing AP, import scopeId:"
                    + entity.getScopeId() + ", existing scopeId:" + ap.getScopeId());
        }
        if (!entity.getApTypeId().equals(ap.getApTypeId())) {
            throw new DEImportException("Type of importing AP doesn't match with type of existing AP, import typeId:"
                    + entity.getApTypeId() + ", existing typeId:" + ap.getApTypeId());
        }
        // TODO: implement how to detect older AP and which versionable sub-entity
        // should be updated.
        entity = ap;
        apInfo.setPersistType(PersistType.UPDATE);
        afterEntityPersist();
        return true;

    }

    @Override
    public void afterEntityPersist() {
        apInfo.setEntityId(entity.getAccessPointId());
        apInfo.onEntityPersist(getMemoryScore());
    }
}
