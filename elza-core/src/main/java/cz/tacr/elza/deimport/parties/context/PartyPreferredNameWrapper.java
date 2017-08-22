package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;

public class PartyPreferredNameWrapper implements EntityWrapper, EntityMetrics {

    private final PartyImportInfo partyInfo;

    private final StatefulIdHolder partyNameIdHolder;

    private ParParty partyRef;

    PartyPreferredNameWrapper(PartyImportInfo partyInfo, StatefulIdHolder partyNameIdHolder) {
        this.partyInfo = Objects.requireNonNull(partyInfo);
        this.partyNameIdHolder = Objects.requireNonNull(partyNameIdHolder);
    }

    @Override
    public boolean isCreate() {
        return false;
    }

    @Override
    public boolean isUpdate() {
        return !partyInfo.getState().equals(State.IGNORE);
    }

    @Override
    public ParParty getEntity() {
        return partyRef;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        partyRef = partyInfo.getUpdatablePartyRef(session);
        partyRef.setPreferredName(partyNameIdHolder.getEntityRef(session, ParPartyName.class));
    }
}
