package cz.tacr.elza.dataexchange.output.parties;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;

public class PartyGroupIdentifierDispatcher extends NestedLoadDispatcher<ParPartyGroupIdentifier> {

    // --- fields ---

    private final List<ParPartyGroupIdentifier> partyGroupIdentifiers = new ArrayList<>();

    private final ParPartyGroup partyGroup;

    // --- constructor ---

    public PartyGroupIdentifierDispatcher(ParPartyGroup partyGroup, LoadDispatcher<PartyInfo> partyInfoDispatcher) {
        super(partyInfoDispatcher);
        this.partyGroup = partyGroup;
    }

    // --- methods ---

    @Override
    public void onLoad(ParPartyGroupIdentifier result) {
        partyGroupIdentifiers.add(result);
    }

    @Override
    public void onCompleted() {
        if (partyGroupIdentifiers.isEmpty()) {
            partyGroup.setPartyGroupIdentifiers(null);
        } else {
            partyGroup.setPartyGroupIdentifiers(partyGroupIdentifiers);
        }
    }
}
