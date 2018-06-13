package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class PartyNameCmplWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyNameComplement entity;

    private final EntityIdHolder<ParPartyName> nameIdHolder;

    private final PartyInfo partyInfo;

    PartyNameCmplWrapper(ParPartyNameComplement entity, EntityIdHolder<ParPartyName> nameIdHolder,
            PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.nameIdHolder = Validate.notNull(nameIdHolder);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    @Override
    public PersistType getPersistType() {
        PersistType pt = partyInfo.getPersistType();
        // name complement is never updated and old must be deleted by storage
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.CREATE;
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
        entity.setPartyName(nameIdHolder.getEntityRef(session));
    }

    @Override
    public void afterEntityPersist() {
        partyInfo.onEntityPersist(getMemoryScore());
    }
}
