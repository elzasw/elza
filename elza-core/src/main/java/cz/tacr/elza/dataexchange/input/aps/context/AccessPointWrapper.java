package cz.tacr.elza.dataexchange.input.aps.context;

import java.time.LocalDateTime;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.projection.RegRecordInfo;

/**
 * Access point (i.e. record) entity wrapper.
 */
public class AccessPointWrapper implements EntityWrapper, EntityMetrics {

    private final RegRecord entity;

    private final AccessPointInfo apInfo;

    private final AccessPointInfo parentAPInfo;

    private PersistMethod persistMethod = PersistMethod.CREATE;

    AccessPointWrapper(RegRecord entity, AccessPointInfo apInfo, AccessPointInfo parentAPInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
        this.parentAPInfo = parentAPInfo;
    }

    /**
     * Updates wrapped entity by paired AP. Uuid, version and apId are copied. When pair is older
     * then imported entity then pair will be updated otherwise no operation is needed.
     *
     * @throws DEImportException When scopes does not match.
     */
    public void setPair(RegRecordInfo pair) {
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
    public RegRecord getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getParentRecord() == null);
        if (parentAPInfo != null) {
            entity.setParentRecord(parentAPInfo.getEntityReference(session));
        }
    }

    @Override
    public void afterEntityPersist() {
        apInfo.setEntityId(entity.getRecordId());
        apInfo.setPersistMethod(persistMethod);
    }
}
