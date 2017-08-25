package cz.tacr.elza.deimport.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
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
        this.entity = Validate.notNull(entity);
        this.partyGroupInfo = Validate.notNull(partyGroupInfo);
    }

    public void setFrom(StatefulIdHolder fromIdHolder) {
        this.fromIdHolder = fromIdHolder;
    }

    public void setTo(StatefulIdHolder toIdHolder) {
        this.toIdHolder = toIdHolder;
    }

    @Override
    public EntityState getState() {
        return partyGroupInfo.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.CREATE;
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
        Validate.isTrue(entity.getPartyGroup() == null);
        entity.setPartyGroup(partyGroupInfo.getEntityRef(session, ParPartyGroup.class));
        // from relation
        Validate.isTrue(entity.getFrom() == null);
        if (fromIdHolder != null) {
            entity.setFrom(fromIdHolder.getEntityRef(session, ParUnitdate.class));
        }
        // to relation
        Validate.isTrue(entity.getTo() == null);
        if (toIdHolder != null) {
            entity.setTo(toIdHolder.getEntityRef(session, ParUnitdate.class));
        }
    }
}
