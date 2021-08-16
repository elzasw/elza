package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApState;

public class SyncEntityRequest {
    final ApAccessPoint accessPoint;

    final EntityXml entityXml;

    ApState state;

    ApBindingState bindingState;

    public SyncEntityRequest(final ApAccessPoint accessPoint, final EntityXml entityXml) {
        this.accessPoint = accessPoint;
        this.entityXml = entityXml;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public ApState getState() {
        return state;
    }

    public void setState(ApState state) {
        this.state = state;
    }

    public ApBindingState getBindingState() {
        return bindingState;
    }

    public void setBindingState(ApBindingState bindingState) {
        this.bindingState = bindingState;
    }

    public EntityXml getEntityXml() {
        return entityXml;
    }
}
