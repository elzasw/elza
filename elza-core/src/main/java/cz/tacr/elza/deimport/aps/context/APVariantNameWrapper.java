package cz.tacr.elza.deimport.aps.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;

/**
 * Access point variant name (i.e. variant record) wrapper.
 */
public class APVariantNameWrapper implements EntityWrapper, EntityMetrics {

    private final RegVariantRecord entity;

    private final RecordImportInfo recordInfo;

    APVariantNameWrapper(RegVariantRecord entity, RecordImportInfo recordInfo) {
        this.entity = Objects.requireNonNull(entity);
        this.recordInfo = Objects.requireNonNull(recordInfo);
    }

    @Override
    public EntityState getState() {
        return recordInfo.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.CREATE;
    }

    @Override
    public RegVariantRecord getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Assert.isNull(entity.getRegRecord());
        entity.setRegRecord(recordInfo.getEntityRef(session, RegRecord.class));
    }
}
