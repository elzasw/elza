package cz.tacr.elza.dataexchange.output.parties;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;

import java.util.Collection;

public class PartyInfo implements BaseApInfo, ExternalIdApInfo {

    // --- fields ---

    private ApState apState;
    private Collection<ApExternalId> externalIds;

    // --- getters/setters ---

    @Override
    public ApState getApState() {
        return apState;
    }

    public void setApState(ApState apState) {
        this.apState = apState;
    }

    @Override
    public Collection<ApExternalId> getExternalIds() {
        return externalIds;
    }

    @Override
    public void setExternalIds(Collection<ApExternalId> externalIds) {
        this.externalIds = externalIds;
    }

    // --- constructor ---
}
