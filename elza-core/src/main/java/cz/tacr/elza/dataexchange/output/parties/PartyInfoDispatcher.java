package cz.tacr.elza.dataexchange.output.parties;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;

public abstract class PartyInfoDispatcher extends BaseLoadDispatcher<PartyInfo> {

    // --- fields ---

    private final StaticDataProvider staticData;

    private PartyInfo partyInfo;

    // --- getters/setters ---

    public PartyInfo getPartyInfo() {
        return partyInfo;
    }

    // --- constructor ---

    public PartyInfoDispatcher(StaticDataProvider staticData) {
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(PartyInfo result) {
        // set result
        Validate.isTrue(partyInfo == null);
        partyInfo = result;
    }
}
