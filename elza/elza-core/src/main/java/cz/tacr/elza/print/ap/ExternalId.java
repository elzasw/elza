package cz.tacr.elza.print.ap;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;

public class ExternalId {

    private final String value;
    
    private final ApExternalSystem type;
    
    private ExternalId(String value, ApExternalSystem type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public ApExternalSystem getType() {
        return type;
    }
    
    public static ExternalId newInstance(ApBinding eid, StaticDataProvider staticData) {
        ApExternalSystem type = null;
        if (eid.getApExternalSystem() != null) {
            type = staticData.getApExternalSystemByCode(eid.getApExternalSystem().getCode());
        }
        return new ExternalId(eid.getValue(), type);
    }
}
