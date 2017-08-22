package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class PartyNameComplementWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyNameComplement entity;

    private final StatefulIdHolder partyNameIdHolder;

    PartyNameComplementWrapper(ParPartyNameComplement entity, StatefulIdHolder partyNameIdHolder) {
        this.entity = Objects.requireNonNull(entity);
        this.partyNameIdHolder = Objects.requireNonNull(partyNameIdHolder);
    }

    @Override
    public boolean isCreate() {
        return !partyNameIdHolder.getState().equals(State.IGNORE);
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public ParPartyNameComplement getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Assert.isNull(entity.getPartyName());
        entity.setPartyName(partyNameIdHolder.getEntityRef(session, ParPartyName.class));
    }
}
