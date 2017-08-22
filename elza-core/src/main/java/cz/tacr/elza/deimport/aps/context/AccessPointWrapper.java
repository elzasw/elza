package cz.tacr.elza.deimport.aps.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.RegRecord;

/**
 * Access point (i.e. record) entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper, EntityMetrics {

    private final RegRecord entity;

    private final RecordImportInfo recordInfo;

    private final RecordImportInfo parentRecordInfo;

    private State entityState = State.CREATE;

    AccessPointWrapper(RegRecord entity, RecordImportInfo recordInfo, RecordImportInfo parentRecordInfo) {
        this.entity = Objects.requireNonNull(entity);
        this.recordInfo = Objects.requireNonNull(recordInfo);
        this.parentRecordInfo = parentRecordInfo;
    }

    public void markAsPaired(boolean updateRequired) {
        entityState = updateRequired ? State.UPDATE : State.IGNORE;
    }

    @Override
    public boolean isCreate() {
        return entityState.equals(State.CREATE);
    }

    @Override
    public boolean isUpdate() {
        return entityState.equals(State.UPDATE);
    }

    @Override
    public RegRecord getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        if (parentRecordInfo != null) {
            Assert.isNull(entity.getParentRecord());
            entity.setParentRecord(parentRecordInfo.getEntityRef(session, RegRecord.class));
        }
    }

    @Override
    public void afterEntityPersist() {
        recordInfo.init(entity.getRecordId(), entityState);
    }
}