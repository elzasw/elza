package cz.tacr.elza.print.ap;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;

public class ExternalId {

    private final String value;
    
    private final ApExternalIdType type;
    
    private ExternalId(String value, ApExternalIdType type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public ApExternalIdType getType() {
        return type;
    }
    
    public static ExternalId newInstance(ApExternalId eid, StaticDataProvider staticData) {
        ApExternalIdType type = null;
        if (eid.getExternalIdTypeId() != null) {
            type = staticData.getApEidTypeById(eid.getAccessPointId());
        }
        return new ExternalId(eid.getValue(), type);
    }
}
