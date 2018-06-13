package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper.PersistType;
import cz.tacr.elza.domain.ParParty;

public class PartyInfo extends EntityIdHolder<ParParty> {

    private final String importId;

    private final AccessPointInfo apInfo;

    private final PartyType partyType;

    private final PartiesContext context;

    private int queuedEntityCount;

    private boolean processed;

    private long memoryScore;

    public PartyInfo(String importId, AccessPointInfo apInfo, PartyType partyType, PartiesContext context) {
        super(partyType.getDomainClass(), false);
        this.importId = importId;
        this.apInfo = apInfo;
        this.partyType = partyType;
        this.context = context;
    }

    public String getImportId() {
        return importId;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public PersistType getPersistType() {
        return apInfo.getPersistType();
    }

    public AccessPointInfo getApInfo() {
        return apInfo;
    }

    public long getMemoryScore() {
        return memoryScore;
    }

    public void onProcessed() {
        Validate.isTrue(!processed);
        processed = true;
        // notify context when all entity are persist
        if (queuedEntityCount == 0) {
            context.onPartyFinished(this);
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
            context.onPartyFinished(this);
        }
    }
}
