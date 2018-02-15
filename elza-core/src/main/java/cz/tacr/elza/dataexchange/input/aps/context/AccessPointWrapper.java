package cz.tacr.elza.dataexchange.input.aps.context;

import java.time.LocalDateTime;

import cz.tacr.elza.domain.ApRecord;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.projection.ApRecordInfo;

/**
 * Access point (i.e. record) entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper, EntityMetrics {

    private final ApRecord entity;

    private final AccessPointInfo apInfo;

    private PersistMethod persistMethod = PersistMethod.CREATE;

    AccessPointWrapper(ApRecord entity, AccessPointInfo apInfo, AccessPointInfo parentAPInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
    }

    /**
     * Updates wrapped entity by paired AP. Uuid, version and apId are copied. When pair is older
     * then imported entity then pair will be updated otherwise no operation is needed.
     *
     * @throws DEImportException When scopes does not match.
     */
    public void setPair(ApRecordInfo pair) {
        if (!entity.getScopeId().equals(pair.getScopeId())) {
            throw new DEImportException("Import scope doesn't match with scope of paired record, import scopeId:"
                    + entity.getScopeId() + ", paired scopeId:" + pair.getScopeId());
        }

        entity.setUuid(pair.getUuid());
        entity.setVersion(pair.getVersion());
        entity.setRecordId(pair.getRecordId());

        if (entity.getLastUpdate() == null) {
            entity.setLastUpdate(LocalDateTime.now());
            persistMethod = PersistMethod.UPDATE;
        } else if (entity.getLastUpdate().isAfter(pair.getLastUpdate())) {
            persistMethod = PersistMethod.UPDATE;
        } else {
            persistMethod = PersistMethod.NONE;
            afterEntityPersist(); // set info immediately
        }
    }

    @Override
    public PersistMethod getPersistMethod() {
        return persistMethod;
    }

    @Override
    public ApRecord getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {}

    @Override
    public void afterEntityPersist() {
        apInfo.setEntityId(entity.getRecordId());
        apInfo.setPersistMethod(persistMethod);
    }
}
