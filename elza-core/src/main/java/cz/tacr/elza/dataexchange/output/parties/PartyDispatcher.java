package cz.tacr.elza.dataexchange.output.parties;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.domain.ParParty;

public abstract class PartyDispatcher extends BaseLoadDispatcher<ParParty> {

    private ParParty party;

    public ParParty getParty() {
        return party;
    }

    @Override
    public void onLoad(ParParty result) {
        Validate.isTrue(party == null);
        party = result;
    }
}
