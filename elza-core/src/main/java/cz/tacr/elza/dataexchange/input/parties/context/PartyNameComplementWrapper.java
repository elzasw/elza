package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class PartyNameComplementWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyNameComplement entity;

    private final PartyRelatedIdHolder<ParPartyName> partyNameIdHolder;

    PartyNameComplementWrapper(ParPartyNameComplement entity, PartyRelatedIdHolder<ParPartyName> partyNameIdHolder) {
        this.entity = Validate.notNull(entity);
        this.partyNameIdHolder = Validate.notNull(partyNameIdHolder);
    }

    @Override
    public PersistMethod getPersistMethod() {
        return partyNameIdHolder.getPartyInfo().isIgnored() ? PersistMethod.NONE : PersistMethod.CREATE;
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
        entity.setPartyName(partyNameIdHolder.getEntityReference(session));
    }
}
