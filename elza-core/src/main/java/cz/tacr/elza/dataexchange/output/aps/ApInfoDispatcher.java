package cz.tacr.elza.dataexchange.output.aps;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;

public abstract class ApInfoDispatcher extends BaseLoadDispatcher<ApInfoImpl> {

    private final StaticDataProvider staticData;

    private ApInfoImpl apInfo;

    public ApInfoDispatcher(StaticDataProvider staticData) {
        this.staticData = staticData;
    }

    public ApInfoImpl getApInfo() {
        return apInfo;
    }

    @Override
    public void onLoad(ApInfoImpl result) {
        // init AP type
        ApAccessPoint ap = result.getAp();
        // todo[dataexchange]: ApState se nikde neplni
        ApState apState = result.getApState();
        ApType apType = staticData.getApTypeById(apState.getApTypeId());
        apState.setApType(Validate.notNull(apType));
        // set result
        Validate.isTrue(apInfo == null);
        apInfo = result;
    }
}
