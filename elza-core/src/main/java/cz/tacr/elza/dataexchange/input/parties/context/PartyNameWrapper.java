package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyNameWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyName entity;

    private final PartyInfo partyInfo;

    private final EntityIdHolder<ParPartyName> idHolder;

    private EntityIdHolder<ParUnitdate> validFromIdHolder;

    private EntityIdHolder<ParUnitdate> validToIdHolder;

    PartyNameWrapper(ParPartyName entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
        this.idHolder = new EntityIdHolder<>(ParPartyName.class, false);
    }

    public EntityIdHolder<ParPartyName> getIdHolder() {
        return idHolder;
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
        // name is never updated and old must be deleted by storage
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.CREATE;
    }

    @Override
    public ParPartyName getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // party relation
        Validate.isTrue(entity.getParty() == null);
        entity.setParty(partyInfo.getEntityRef(session));
        // valid from relation
        Validate.isTrue(entity.getValidFrom() == null);
        if (validFromIdHolder != null) {
            entity.setValidFrom(validFromIdHolder.getEntityRef(session));
        }
        // valid to relation
        Validate.isTrue(entity.getValidTo() == null);
        if (validToIdHolder != null) {
            entity.setValidTo(validToIdHolder.getEntityRef(session));
        }
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getPartyNameId());
        partyInfo.onEntityPersist(getMemoryScore());
    }
}
