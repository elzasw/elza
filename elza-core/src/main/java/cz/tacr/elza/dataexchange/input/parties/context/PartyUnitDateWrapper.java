package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyUnitDateWrapper implements EntityWrapper, EntityMetrics {

    private final ParUnitdate entity;

    private final PartyInfo partyInfo;

    private final EntityIdHolder<ParUnitdate> idHolder;

    PartyUnitDateWrapper(ParUnitdate entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
        this.idHolder = new EntityIdHolder<>(ParUnitdate.class, false);
    }

    public EntityIdHolder<ParUnitdate> getIdHolder() {
        return idHolder;
    }

    @Override
    public PersistType getPersistType() {
        PersistType pt = partyInfo.getPersistType();
        // unit date is never updated and old must be deleted by storage
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.CREATE;
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
    public void afterEntityPersist() {
        idHolder.setEntityId(entity.getUnitdateId());
        partyInfo.onEntityPersist(getMemoryScore());
    }
}
