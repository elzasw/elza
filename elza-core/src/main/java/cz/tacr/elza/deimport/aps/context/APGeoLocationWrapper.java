package cz.tacr.elza.deimport.aps.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegRecord;

/**
 * Access point geo location (i.e. coordinates) wrapper.
 */
public class APGeoLocationWrapper implements EntityWrapper, EntityMetrics {

    private final RegCoordinates entity;

    private final RecordImportInfo recordInfo;

    APGeoLocationWrapper(RegCoordinates entity, RecordImportInfo recordInfo) {
        this.entity = Validate.notNull(entity);
        this.recordInfo = Validate.notNull(recordInfo);
    }

    @Override
    public EntityState getState() {
        return recordInfo.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.CREATE;
    }

    @Override
    public RegCoordinates getEntity() {
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
