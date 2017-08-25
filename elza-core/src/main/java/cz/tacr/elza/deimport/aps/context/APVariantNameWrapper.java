package cz.tacr.elza.deimport.aps.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

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
        this.entity = Validate.notNull(entity);
        this.recordInfo = Validate.notNull(recordInfo);
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
        Validate.isTrue(entity.getRegRecord() == null);
        entity.setRegRecord(recordInfo.getEntityRef(session, RegRecord.class));
    }
}
