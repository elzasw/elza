package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;

import java.util.Collection;

public class ApInfo implements BaseApInfo, ExternalIdApInfo {

    // --- fields ---

    private final ApState apState;

    private Collection<ApExternalId> externalIds;

    private boolean partyAp;

    // --- getters/setters ---

    @Override
    public ApState getApState() {
        return apState;
    }

    @Override
    public Collection<ApExternalId> getExternalIds() {
        return externalIds;
    }

    @Override
    public void setExternalIds(Collection<ApExternalId> externalIds) {
        this.externalIds = externalIds;
    }

    public boolean isPartyAp() {
        return partyAp;
    }

    public void setPartyAp(boolean partyAp) {
        this.partyAp = partyAp;
    }

    // --- constructor ---

    public ApInfo(ApState apState) {
        this.apState = apState;
    }
}
