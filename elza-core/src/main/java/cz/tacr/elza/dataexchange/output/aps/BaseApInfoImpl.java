package cz.tacr.elza.dataexchange.output.aps;

import java.util.Collection;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;

public class BaseApInfoImpl implements BaseApInfo {

    private final ApAccessPoint ap;
    private final ApState apState;

    private Collection<ApExternalId> externalIds;

    public BaseApInfoImpl(ApAccessPoint ap) {
        this.ap = ap;
        // todo[dataexchange]: ApState se neplni
        this.apState = null;
    }

    @Override
    public ApAccessPoint getAp() {
        return ap;
    }

    @Override
    public ApState getApState() {
        return apState;
    }

    @Override
    public Collection<ApExternalId> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(Collection<ApExternalId> externalIds) {
        this.externalIds = externalIds;
    }
}
