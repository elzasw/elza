package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.SimpleStatefulIdHolder;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyUnitDateWrapper implements EntityWrapper, EntityMetrics {

    private final ParUnitdate entity;

    private final SimpleStatefulIdHolder idHolder;

    PartyUnitDateWrapper(ParUnitdate entity, RecordImportInfo recordInfo) {
        this.entity = Objects.requireNonNull(entity);
        this.idHolder = new SimpleStatefulIdHolder(ParUnitdate.class, recordInfo);
    }

    public StatefulIdHolder getHolderId() {
        return idHolder;
    }

    @Override
    public EntityState getState() {
        return idHolder.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.CREATE;
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
        idHolder.setId(entity.getUnitdateId());
    }
}
