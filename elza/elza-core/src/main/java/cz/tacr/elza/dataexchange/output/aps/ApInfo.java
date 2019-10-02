package cz.tacr.elza.dataexchange.output.aps;

import java.util.Collection;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApState;

public class ApInfo implements BaseApInfo, ExternalIdApInfo {

    // --- fields ---

    private final ApState apState;

    private Collection<ApExternalId> externalIds;

    private Collection<ApName> names;

    private ApDescription desc;

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

    public Collection<ApName> getNames() {
        return names;
    }

    public void setNames(Collection<ApName> names) {
        this.names = names;
    }

    public ApDescription getDesc() {
        return desc;
    }

    public void setDesc(ApDescription desc) {
        this.desc = desc;
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
