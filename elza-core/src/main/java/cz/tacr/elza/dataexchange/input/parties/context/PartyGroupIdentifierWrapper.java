package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyGroupIdentifierWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyGroupIdentifier entity;

    private final PartyInfo partyInfo;

    private EntityIdHolder<ParUnitdate> validFromIdHolder;

    private EntityIdHolder<ParUnitdate> validToIdHolder;

    PartyGroupIdentifierWrapper(ParPartyGroupIdentifier entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    public void setValidFrom(EntityIdHolder<ParUnitdate> validFromIdHolder) {
        this.validFromIdHolder = validFromIdHolder;
    }

    public void setValidTo(EntityIdHolder<ParUnitdate> validToIdHolder) {
        this.validToIdHolder = validToIdHolder;
    }

    @Override
    public PersistType getPersistType() {
        PersistType pt = partyInfo.getPersistType();
        // group identifier is never updated and old must be deleted by storage
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.CREATE;
    }

    @Override
    public ParPartyGroupIdentifier getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // party group relation
        Validate.isTrue(entity.getPartyGroup() == null);
        entity.setPartyGroup((ParPartyGroup) partyInfo.getEntityRef(session));
        // valid from relation
        Validate.isTrue(entity.getFrom() == null);
        if (validFromIdHolder != null) {
            entity.setFrom(validFromIdHolder.getEntityRef(session));
        }
        // valid to relation
        Validate.isTrue(entity.getTo() == null);
        if (validToIdHolder != null) {
            entity.setTo(validToIdHolder.getEntityRef(session));
        }
    }

    @Override
    public void afterEntityPersist() {
        partyInfo.onEntityPersist(getMemoryScore());
    }
}
