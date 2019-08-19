package cz.tacr.elza.dataexchange.output.parties;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;

public class NameDispatcher extends NestedLoadDispatcher<ParPartyName> {

    // --- fields ---

    private final List<ParPartyName> names = new ArrayList<>();

    private final ParParty party;

    private final StaticDataProvider staticData;

    // --- constructor ---

    public NameDispatcher(ParParty party, LoadDispatcher<PartyInfo> partyInfoDispatcher, StaticDataProvider staticData) {
        super(partyInfoDispatcher);
        this.party = party;
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(ParPartyName result) {
        // init form type if present
        Integer formTypeId = result.getNameFormTypeId();
        if (formTypeId != null) {
            ParPartyNameFormType formType = staticData.getPartyNameFormTypeById(formTypeId);
            Validate.notNull(formType);
            result.setNameFormType(formType);
        }
        // init references
        if (party.getPreferredNameId().equals(result.getPartyNameId())) {
            party.setPreferredName(result);
        }
        names.add(result);
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
