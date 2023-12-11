package cz.tacr.elza.dataexchange.input.aps.context;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.service.ArrangementService;

/**
 * Access point entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper {

    private final ApAccessPoint entity;

    private final AccessPointInfo apInfo;

    private final Collection<ApBindingState> externalIds;

    private final ArrangementService arrangementService;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    AccessPointWrapper(ApAccessPoint entity,
                       AccessPointInfo apInfo,
                       Collection<ApBindingState> externalIds,
                       ArrangementService arrangementService) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
        this.externalIds = externalIds;
        this.arrangementService = arrangementService;
    }

    @Override
    public SaveMethod getSaveMethod() {
        return saveMethod;
    }

    @Override
    public ApAccessPoint getEntity() {
        return entity;
    }

    public ApState getApState() {
        return apInfo.getApState();
    }

    public AccessPointInfo getApInfo() {
        return apInfo;
    }

    public Collection<ApBindingState> getExternalIds() {
        return externalIds;
    }

    /**
     * Updates wrapper with given AP. When given AP is older then importing entity
     * no operation is needed.
     *
     * @throws DEImportException When scopes or types does not match.
     */
    public void changeToUpdated(ApAccessPointInfo dbInfo) {
        // check if item is not already processed
        Validate.isTrue(saveMethod != SaveMethod.UPDATE);

        // access point id is valid (not null)
        int accessPointId = dbInfo.getAccessPointId();

        Integer entityScopeId = getApState().getScopeId();
        if (!entityScopeId.equals(dbInfo.getApScopeId())) {
            throw new DEImportException("Scope of importing AP doesn't match with scope of existing AP, import scopeId:"
                    + entityScopeId + ", existing scopeId:" + dbInfo.getApScopeId());
        }
        Integer entityTypeId = getApState().getApTypeId();
        if (!entityTypeId.equals(dbInfo.getApTypeId())) {
            throw new DEImportException("Type of importing AP doesn't match with type of existing AP, import typeId:"
                    + entityTypeId + ", existing typeId:" + dbInfo.getApTypeId());
        }
        // TODO: implement how to detect older AP and which versionable sub-entity
        // should be updated.
        saveMethod = SaveMethod.UPDATE;
        apInfo.setSaveMethod(saveMethod);
        entity.setAccessPointId(accessPointId);
        apInfo.setEntityId(accessPointId);
        apInfo.onEntityPersist();
    }

    @Override
    public void persist(Session session) {
        session.persist(getEntity());
        session.persist(getApState());
    }

    @Override
    public void merge(Session session) {
        // actual AP update is not needed
    }

    @Override
    public void evictFrom(Session session) {
        session.evict(getApState());
        session.evict(getEntity());
    }

    @Override
    public void beforeEntitySave(Session session) {
        // generate UUID
        if (StringUtils.isEmpty(entity.getUuid())) {
            entity.setUuid(arrangementService.generateUuid());
        }
    }

    @Override
    public void afterEntitySave(Session session) {
        // update AP info
        apInfo.setSaveMethod(saveMethod);
        apInfo.setEntityId(entity.getAccessPointId());
        apInfo.onEntityPersist();
    }
}
