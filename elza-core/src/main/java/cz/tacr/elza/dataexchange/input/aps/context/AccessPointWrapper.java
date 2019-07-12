package cz.tacr.elza.dataexchange.input.aps.context;

import java.util.Collection;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.service.ArrangementService;

/**
 * Access point entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper {

    private final ApAccessPoint entity;

    // todo[dataexchange]: odstranit ApState
    private final ApState apState;

    private final AccessPointInfo apInfo;

    private final Collection<ApExternalId> externalIds;

    private final ArrangementService arrangementService;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    AccessPointWrapper(ApAccessPoint entity,
                       ApState apState,
                       AccessPointInfo apInfo,
                       Collection<ApExternalId> externalIds,
                       ArrangementService arrangementService) {
        this.entity = Validate.notNull(entity);
        this.apState = apState;
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
        return apInfo.getState();
    }

    public Collection<ApExternalId> getExternalIds() {
        return externalIds;
    }

    /**
     * Updates wrapper with given AP. When given AP is older then importing entity
     * no operation is needed.
     * 
     * @throws DEImportException
     *             When scopes or types does not match.
     */
    public void changeToUpdated(ApAccessPointInfo dbInfo) {
        // check if item is not already processed
        Validate.isTrue(saveMethod != SaveMethod.UPDATE);

        // access point id is valid (not null)
        int accessPointId = dbInfo.getAccessPointId();

        // todo[dataexchange]: ApState se nikde neplni
        // Integer entityScopeId = entity.getScopeId();
        Integer entityScopeId = apState.getScopeId();
        if (!entityScopeId.equals(dbInfo.getScopeId())) {
            throw new DEImportException("Scope of importing AP doesn't match with scope of existing AP, import scopeId:"
                    + entityScopeId + ", existing scopeId:" + dbInfo.getScopeId());
        }
        // todo[dataexchange]: ApState se nikde neplni
        // Integer entityTypeId = entity.getApTypeId();
        Integer entityTypeId = apState.getApTypeId();
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
    public void beforeEntitySave(Session session) {
        // generate UUID
        entity.setUuid(arrangementService.generateUuid());
    }

    @Override
    public void afterEntitySave() {
        // update AP info
        apInfo.setSaveMethod(saveMethod);
        apInfo.setEntityId(entity.getAccessPointId());
        apInfo.onEntityPersist();
    }
}
