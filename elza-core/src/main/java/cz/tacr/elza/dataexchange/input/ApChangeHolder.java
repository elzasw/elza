package cz.tacr.elza.dataexchange.input;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApChange.Type;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointService;

/**
 * AP change holder for lazy initialization.
 */
public class ApChangeHolder {

    private final AccessPointDataService apDataService;

    private ApChange change;

    public ApChangeHolder(AccessPointDataService apDataService) {
        this.apDataService = apDataService;
    }

    public ApChange getChange() {
        if (change == null) {
            change = apDataService.createChange(Type.AP_IMPORT);
        }
        return change;
    }
}
