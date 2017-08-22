package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyGroupIdentifierWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyGroupIdentifier entity;

    private final PartyImportInfo partyGroupInfo;

    private StatefulIdHolder fromIdHolder;

    private StatefulIdHolder toIdHolder;

    PartyGroupIdentifierWrapper(ParPartyGroupIdentifier entity, PartyImportInfo partyGroupInfo) {
        this.entity = Objects.requireNonNull(entity);
        this.partyGroupInfo = Objects.requireNonNull(partyGroupInfo);
    }

    public void setFrom(StatefulIdHolder fromIdHolder) {
        this.fromIdHolder = fromIdHolder;
    }

    public void setTo(StatefulIdHolder toIdHolder) {
        this.toIdHolder = toIdHolder;
    }

    @Override
    public boolean isCreate() {
        return !partyGroupInfo.getState().equals(State.IGNORE);
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public ParPartyGroupIdentifier getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // party group relation
        Assert.isNull(entity.getPartyGroup());
        entity.setPartyGroup(partyGroupInfo.getEntityRef(session, ParPartyGroup.class));
        // from relation
        Assert.isNull(entity.getFrom());
        if (fromIdHolder != null) {
            entity.setFrom(fromIdHolder.getEntityRef(session, ParUnitdate.class));
        }
        // to relation
        Assert.isNull(entity.getTo());
        if (toIdHolder != null) {
            entity.setTo(toIdHolder.getEntityRef(session, ParUnitdate.class));
        }
    }
}
