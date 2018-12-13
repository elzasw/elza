package cz.tacr.elza.dataexchange.input;

import org.hibernate.Session;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApChange.Type;
import cz.tacr.elza.service.AccessPointDataService;

/**
 * AP change holder for lazy initialization.
 */
public class ApChangeHolder {

    private final AccessPointDataService apDataService;

    private ApChange change;

    private Session session;

    public ApChangeHolder(AccessPointDataService apDataService, Session session) {
        this.apDataService = apDataService;
        this.session = session;
    }

    public ApChange getChange() {
        if (change == null) {
            change = apDataService.createChange(Type.AP_IMPORT);
            // we have to be sure that change is stored in DB
            // before another operation            
            session.flush();
        }
        return change;
    }
}
