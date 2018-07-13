package cz.tacr.elza.dataexchange.output.parties;

import cz.tacr.elza.dataexchange.output.aps.BaseApInfoImpl;
import cz.tacr.elza.dataexchange.output.writer.PartyInfo;
import cz.tacr.elza.domain.ParParty;

public class PartyInfoImpl implements PartyInfo {

    private final ParParty party;

    private BaseApInfoImpl baseApInfo;

    public PartyInfoImpl(ParParty party) {
        this.party = party;
    }

    @Override
    public BaseApInfoImpl getBaseApInfo() {
        return baseApInfo;
    }

    public void setBaseApInfo(BaseApInfoImpl baseApInfo) {
        this.baseApInfo = baseApInfo;
    }

    @Override
    public ParParty getParty() {
        return party;
    }
}
