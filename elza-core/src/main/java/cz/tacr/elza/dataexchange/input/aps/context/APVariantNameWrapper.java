package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.domain.ApVariantRecord;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;

/**
 * Access point variant name (i.e. variant record) wrapper.
 */
public class APVariantNameWrapper implements EntityWrapper, EntityMetrics {

    private final ApVariantRecord entity;

    private final AccessPointInfo apInfo;

    APVariantNameWrapper(ApVariantRecord entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    @Override
    public PersistMethod getPersistMethod() {
        return apInfo.isIgnored() ? PersistMethod.NONE : PersistMethod.CREATE;
    }

    @Override
    public ApVariantRecord getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getApRecord() == null);
        entity.setApRecord(apInfo.getEntityReference(session));
    }
}
