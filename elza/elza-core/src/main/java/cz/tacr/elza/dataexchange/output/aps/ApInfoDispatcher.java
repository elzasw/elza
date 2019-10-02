package cz.tacr.elza.dataexchange.output.aps;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;

public abstract class ApInfoDispatcher extends BaseLoadDispatcher<ApInfo> {

    // --- fields ---

    private final StaticDataProvider staticData;

    private ApInfo apInfo;

    // --- getters/setters ---

    public ApInfo getApInfo() {
        return apInfo;
    }

    // --- constructor ---

    public ApInfoDispatcher(StaticDataProvider staticData) {
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(ApInfo result) {
        // init AP type
        ApState apState = result.getApState();
        ApType apType = staticData.getApTypeById(apState.getApTypeId());
        apState.setApType(Validate.notNull(apType));
        // set result
        Validate.isTrue(apInfo == null);
        apInfo = result;
    }
}
