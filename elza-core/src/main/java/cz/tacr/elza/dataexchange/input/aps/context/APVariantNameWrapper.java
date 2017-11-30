package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.RegVariantRecord;

/**
 * Access point variant name (i.e. variant record) wrapper.
 */
public class APVariantNameWrapper implements EntityWrapper, EntityMetrics {

    private final RegVariantRecord entity;

    private final AccessPointInfo apInfo;

    APVariantNameWrapper(RegVariantRecord entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    @Override
    public PersistMethod getPersistMethod() {
        return apInfo.isIgnored() ? PersistMethod.NONE : PersistMethod.CREATE;
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
        entity.setRegRecord(apInfo.getEntityReference(session));
    }
}
