package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.service.ArrangementService;

/**
 * Access point entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper {

    private final ApAccessPoint entity;

    private final AccessPointInfo apInfo;

    private final MultiValuedMap<String, String> eidTypeValuedMap;

    private final ArrangementService arrangementService;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    AccessPointWrapper(ApAccessPoint entity, AccessPointInfo apInfo, MultiValuedMap<String, String> eidTypeValueMap,
            ArrangementService arrangementService) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
        this.eidTypeValuedMap = eidTypeValueMap;
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

    public MultiValuedMap<String, String> getEidTypeValueMap() {
        return eidTypeValuedMap;
    }

    /**
     * Updates wrapper with given AP. When given AP is older then importing entity
     * no operation is needed.
     * 
     * @throws DEImportException
     *             When scopes or types does not match.
     */
    public void changeToUpdated(ApAccessPointInfo info) {
        if (!entity.getScopeId().equals(info.getScopeId())) {
            throw new DEImportException("Scope of importing AP doesn't match with scope of existing AP, import scopeId:"
                    + entity.getScopeId() + ", existing scopeId:" + info.getScopeId());
        }
        if (!entity.getApTypeId().equals(info.getApTypeId())) {
            throw new DEImportException("Type of importing AP doesn't match with type of existing AP, import typeId:"
                    + entity.getApTypeId() + ", existing typeId:" + info.getApTypeId());
        }
        // TODO: implement how to detect older AP and which versionable sub-entity
        // should be updated.
        saveMethod = SaveMethod.UPDATE;
        apInfo.setSaveMethod(saveMethod);
        apInfo.setEntityId(info.getAccessPointId());
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
