package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyGroupIdentifierWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyGroupIdentifier entity;

    private final PartyInfo partyGroupInfo;

    private EntityIdHolder<ParUnitdate> validFromIdHolder;

    private EntityIdHolder<ParUnitdate> validToIdHolder;

    PartyGroupIdentifierWrapper(ParPartyGroupIdentifier entity, PartyInfo partyGroupInfo) {
        this.entity = Validate.notNull(entity);
        this.partyGroupInfo = Validate.notNull(partyGroupInfo);
    }

    public void setValidFrom(EntityIdHolder<ParUnitdate> validFromIdHolder) {
        this.validFromIdHolder = validFromIdHolder;
    }

    public void setValidTo(EntityIdHolder<ParUnitdate> validToIdHolder) {
        this.validToIdHolder = validToIdHolder;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return partyGroupInfo.isIgnored() ? PersistMethod.NONE : PersistMethod.CREATE;
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
        entity.setPartyGroup((ParPartyGroup) partyGroupInfo.getEntityReference(session));
        // valid from relation
        Validate.isTrue(entity.getFrom() == null);
        if (validFromIdHolder != null) {
            entity.setFrom(validFromIdHolder.getEntityReference(session));
        }
        // valid to relation
        Validate.isTrue(entity.getTo() == null);
        if (validToIdHolder != null) {
            entity.setTo(validToIdHolder.getEntityReference(session));
        }
    }
}
