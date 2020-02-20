package cz.tacr.elza.dataexchange.output.parties;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParPartyType;

public abstract class PartyInfoDispatcher extends BaseLoadDispatcher<PartyInfoImpl> {

    private final StaticDataProvider staticData;

    private PartyInfoImpl partyInfo;

    public PartyInfoDispatcher(StaticDataProvider staticData) {
        this.staticData = staticData;
    }

    public PartyInfoImpl getPartyInfo() {
        return partyInfo;
    }

    @Override
    public void onLoad(PartyInfoImpl result) {
        ApAccessPoint ap = result.getBaseApInfo().getAp();
        // get AP type
        Integer apTypeId = ap.getApTypeId();
        ApType apType = staticData.getApTypeById(apTypeId);
        // init AP type
        Validate.notNull(apType);
        ap.setApType(apType);
        // init party type
        ParPartyType partyType = Validate.notNull(apType.getPartyType());
        result.getParty().setPartyType(partyType);
        // set result
        Validate.isTrue(partyInfo == null);
        partyInfo = result;
    }
}