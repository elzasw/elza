package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParParty;

public class PartyInfo implements EntityIdHolder<ParParty> {

    private final String importId;

    private final AccessPointInfo apInfo;

    private final PartyType partyType;

    private final PartiesContext context;

    private Integer entityId;

    private int queuedEntityCount;

    private boolean processed;

    public PartyInfo(String importId, AccessPointInfo apInfo, PartyType partyType, PartiesContext context) {
        this.importId = importId;
        this.apInfo = apInfo;
        this.partyType = partyType;
        this.context = context;
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
    public ParParty getEntityRef(Session session) {
        Validate.notNull(entityId);

        return HibernateUtils.getEntityRef(entityId, ParParty.class, session, false);
    }

    public String getImportId() {
        return importId;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public SaveMethod getSaveMethod() {
        return apInfo.getSaveMethod();
    }

    public AccessPointInfo getApInfo() {
        return apInfo;
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

    public void onEntityPersist() {
        Validate.isTrue(queuedEntityCount > 0);
        queuedEntityCount--;
        // notify context when processed and all entity are persist
        if (processed && queuedEntityCount == 0) {
            context.onPartyFinished(this);
        }
    }
}
