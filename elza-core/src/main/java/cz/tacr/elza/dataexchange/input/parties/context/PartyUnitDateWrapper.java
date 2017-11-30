package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyUnitDateWrapper implements EntityWrapper, EntityMetrics {

    private final ParUnitdate entity;

    private final PartyRelatedIdHolder<ParUnitdate> idHolder;

    PartyUnitDateWrapper(ParUnitdate entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.idHolder = new PartyRelatedIdHolder<>(ParUnitdate.class, partyInfo);
    }

    public PartyRelatedIdHolder<ParUnitdate> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return idHolder.getPartyInfo().isIgnored() ? PersistMethod.NONE : PersistMethod.CREATE;
    }

    @Override
    public ParUnitdate getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getUnitdateId());
    }
}
