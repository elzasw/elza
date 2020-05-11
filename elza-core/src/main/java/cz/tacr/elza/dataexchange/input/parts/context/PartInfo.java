package cz.tacr.elza.dataexchange.input.parts.context;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulStructuredType;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

public class PartInfo implements EntityIdHolder<ApPart> {

    private final String importId;

    private final RulPartType rulPartType;

    private final PartsContext context;

    private AccessPointInfo apInfo;

    private Integer entityId;

    private int queuedEntityCount;

    private boolean processed;

    public PartInfo(String importId, AccessPointInfo apInfo, RulPartType structuredType, PartsContext context) {
        this.importId = importId;
        this.apInfo = apInfo;
        this.rulPartType = structuredType;
        this.context = context;
    }

    public AccessPointInfo getApInfo() {
        return apInfo;
    }

    public void setApInfo(AccessPointInfo apInfo) {
        this.apInfo = apInfo;
    }

    public RulPartType getRulPartType() {
        return rulPartType;
    }

    public PartsContext getContext() {
        return context;
    }

    public SaveMethod getSaveMethod() {
        return apInfo.getSaveMethod();
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public ApPart getEntityRef(Session session) {
        Validate.notNull(entityId);
        return HibernateUtils.getEntityRef(entityId, ApPart.class, session, false);
    }

    public String getImportId() {
        return importId;
    }

    public void onProcessed() {
        Validate.isTrue(!processed);
        processed = true;
        // notify context when all entity are persist
        if (queuedEntityCount == 0) {
            context.onPartFinished(this);
        }
    }

    public void onEntityQueued() {
        Validate.isTrue(!processed);
        queuedEntityCount++;
    }

    public void onEntityPersist() {
        Validate.isTrue(queuedEntityCount > 0);
        queuedEntityCount--;
        // notify context when processed and all entity are persist
        if (processed && queuedEntityCount == 0) {
            context.onPartFinished(this);
        }
    }




}
