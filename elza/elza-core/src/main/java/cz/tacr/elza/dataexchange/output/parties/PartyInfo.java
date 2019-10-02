package cz.tacr.elza.dataexchange.output.parties;

import java.util.Collection;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ParParty;

public class PartyInfo implements BaseApInfo, ExternalIdApInfo {

    // --- fields ---

    private final ParParty party;

    private ApState apState;
    private Collection<ApExternalId> externalIds;

    // --- getters/setters ---

    public ParParty getParty() {
        return party;
    }

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

    public PartyInfo(ParParty party) {
        this.party = party;
    }
}
