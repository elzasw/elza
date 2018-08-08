package cz.tacr.elza.dataexchange.input;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApChange.Type;
import cz.tacr.elza.service.AccessPointService;

/**
 * AP change holder for lazy initialization.
 */
public class ApChangeHolder {

    private final AccessPointService accessPointService;

    private ApChange change;

    public ApChangeHolder(AccessPointService accessPointService) {
        this.accessPointService = accessPointService;
    }

    public ApChange getChange() {
        if (change == null) {
            change = accessPointService.createChange(Type.AP_IMPORT);
        }
        return change;
    }
}
