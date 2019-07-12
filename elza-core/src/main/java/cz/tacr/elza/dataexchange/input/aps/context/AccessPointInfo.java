package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApType;

/**
 * Access point import info which primarily stores id and result of record
 * pairing.
 */
public class AccessPointInfo implements EntityIdHolder<ApAccessPoint> {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointInfo.class);

    private final ApType apType;

    private final ApScope apScope;

    private Integer entityId;

    private SaveMethod saveMethod;

    private String fulltext;

    private int queuedEntityCount;

    private boolean processed;

    public AccessPointInfo(ApType apType, ApScope apScope) {
        this.apType = apType;
        this.apScope = apScope;
    }

    @Override
    public Integer getEntityId() {
        Validate.notNull(entityId);

        return entityId;
    }

    void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public ApAccessPoint getEntityRef(Session session) {
        Validate.notNull(entityId);

        return HibernateUtils.getEntityRef(entityId, ApAccessPoint.class, session, false);
    }

    public ApType getApType() {
        return apType;
    }

    public ApScope getApScope() {
        return apScope;
    }

    public SaveMethod getSaveMethod() {
        return saveMethod;
    }

    void setSaveMethod(SaveMethod saveMethod) {
        this.saveMethod = saveMethod;
    }

    public String getFulltext() {
        return fulltext;
    }

    void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    public void onProcessed() {
        Validate.isTrue(!processed);
        processed = true;
        // AP is finished when all entity are persist
        if (queuedEntityCount == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("AP fninished, entityId={}, fulltext={}", entityId, fulltext);
            }
        }
    }

    public void onEntityQueued() {
        Validate.isTrue(!processed);
        queuedEntityCount++;
    }

    public void onEntityPersist() {
        Validate.isTrue(queuedEntityCount > 0);
        queuedEntityCount--;
        // AP is finished when processed and all entity are persist
        if (processed && queuedEntityCount == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("AP fninished, entityId={}, fulltext={}", entityId, fulltext);
            }
        }
    }
}
