package cz.tacr.elza.deimport.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class PartyNameComplementWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyNameComplement entity;

    private final StatefulIdHolder partyNameIdHolder;

    PartyNameComplementWrapper(ParPartyNameComplement entity, StatefulIdHolder partyNameIdHolder) {
        this.entity = Validate.notNull(entity);
        this.partyNameIdHolder = Validate.notNull(partyNameIdHolder);
    }

    @Override
    public EntityState getState() {
        return partyNameIdHolder.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.CREATE;
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
        Validate.isTrue(entity.getPartyName() == null);
        entity.setPartyName(partyNameIdHolder.getEntityRef(session, ParPartyName.class));
    }
}
