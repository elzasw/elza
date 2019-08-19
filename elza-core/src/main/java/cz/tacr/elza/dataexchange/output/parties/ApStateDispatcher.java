package cz.tacr.elza.dataexchange.output.parties;


import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;

public class ApStateDispatcher extends NestedLoadDispatcher<ApState> {

    // --- fields ---

    private final StaticDataProvider staticData;

    private final PartyInfo party;

    private ApState apState;

    // --- constructor ---

    public ApStateDispatcher(PartyInfo party, LoadDispatcher<?> parentDispatcher, StaticDataProvider staticData) {
        super(parentDispatcher);
        this.party = party;
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(ApState result) {
        // get AP type
        ApType apType = staticData.getApTypeById(result.getApTypeId());
        Validate.notNull(apType);
        // init AP type
        result.setApType(apType);
        // set result
        Validate.isTrue(apState == null);
        apState = result;
    }

    @Override
    protected void onCompleted() {
        party.setApState(apState);
    }
}
