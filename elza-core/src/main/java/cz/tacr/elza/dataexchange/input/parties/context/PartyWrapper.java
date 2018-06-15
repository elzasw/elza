package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ParPartyInfo;

public class PartyWrapper implements EntityWrapper, EntityMetrics {

    private final ParParty entity;

    private final PartyInfo partyInfo;

    PartyWrapper(ParParty entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    public PartyInfo getPartyInfo() {
        return partyInfo;
    }

    @Override
    public PersistType getPersistType() {
        return partyInfo.getPersistType();
    }

    @Override
    public ParParty getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    public void prepareUpdate(ParPartyInfo info) {
        entity.setPartyId(info.getPartyId());
        entity.setVersion(info.getVersion());
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getRecord() == null);
        entity.setRecord(partyInfo.getApInfo().getEntityRef(session));
    }

    @Override
    public void afterEntityPersist() {
        partyInfo.setEntityId(entity.getPartyId());
        partyInfo.onEntityPersist(getMemoryScore());
    }
}
