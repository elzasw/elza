package cz.tacr.elza.dataexchange.output.parties;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;

public class NameDispatcher extends NestedLoadDispatcher<ParPartyName> {

    private final List<ParPartyName> names = new ArrayList<>();

    private final ParParty party;

    public NameDispatcher(ParParty party, LoadDispatcher<ParParty> partyDispatcher) {
        super(partyDispatcher);
        this.party = party;
    }

    @Override
    public void onLoad(ParPartyName result) {
        if (party.getPreferredNameId().equals(result.getPartyNameId())) {
            party.setPreferredName(result);
        } else {
            names.add(result);
        }
    }

    @Override
    public void onCompleted() {
        Validate.isTrue(HibernateUtils.isInitialized(party.getPreferredName()));
        if (names.isEmpty()) {
            party.setPartyNames(null);
        } else {
            party.setPartyNames(names);
        }
    }
}
