package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.ApType;

public class ApTypeRoles {

    private final ApType type;

    ApTypeRoles(ApType type) {
       this.type = type;
    }

    public ApType getType() {
        return type;
    }
}
