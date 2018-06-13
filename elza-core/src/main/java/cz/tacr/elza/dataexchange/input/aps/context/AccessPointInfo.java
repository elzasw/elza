package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.lang3.Validate;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper.PersistType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApType;

/**
 * Access point import info which primarily stores id and result of record
 * pairing.
 */
public class AccessPointInfo extends EntityIdHolder<ApAccessPoint> {

    private final String entryId;

    private final ApType apType;

    private final AccessPointsContext context;

    private PersistType persistType = PersistType.CREATE;

    private String fulltext;

    private int queuedEntityCount;

    private boolean processed;

    private long memoryScore;

    public AccessPointInfo(String entryId, ApType apType, AccessPointsContext context) {
        super(ApAccessPoint.class, false);
        this.entryId = entryId;
        this.apType = apType;
        this.context = context;
    }

    public String getEntryId() {
        return entryId;
    }

    public ApType getApType() {
        return apType;
    }

    public PersistType getPersistType() {
        return persistType;
    }

    public void setPersistType(PersistType persistType) {
        this.persistType = persistType;
    }

    public String getFulltext() {
        return fulltext;
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    public long getMemoryScore() {
        return memoryScore;
    }

    public void onProcessed() {
        Validate.isTrue(!processed);
        processed = true;
        // notify context when all entity are persist
        if (queuedEntityCount == 0) {
            context.onAccessPointFinished(this);
        }
    }

    public void onEntityQueued() {
        Validate.isTrue(!processed);
        queuedEntityCount++;
    }

    public void onEntityPersist(long memoryScore) {
        Validate.isTrue(queuedEntityCount > 0);
        this.memoryScore += memoryScore;
        queuedEntityCount--;
        // notify context when processed and all entity are persist
        if (processed && queuedEntityCount == 0) {
            context.onAccessPointFinished(this);
        }
    }
}
