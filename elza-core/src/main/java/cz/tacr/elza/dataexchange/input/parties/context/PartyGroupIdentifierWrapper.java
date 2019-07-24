package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyGroupIdentifierWrapper implements EntityWrapper {

    private final ParPartyGroupIdentifier entity;

    private final PartyInfo partyInfo;

    private EntityIdHolder<ParUnitdate> validFromIdHolder;

    private EntityIdHolder<ParUnitdate> validToIdHolder;

    PartyGroupIdentifierWrapper(ParPartyGroupIdentifier entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    public void setValidFrom(EntityIdHolder<ParUnitdate> validFromIdHolder) {
        this.validFromIdHolder = validFromIdHolder;
    }

    public void setValidTo(EntityIdHolder<ParUnitdate> validToIdHolder) {
        this.validToIdHolder = validToIdHolder;
    }

    @Override
    public SaveMethod getSaveMethod() {
        SaveMethod sm = partyInfo.getSaveMethod();
        // group identifier is never updated and old must be invalidate by storage
        return sm.equals(SaveMethod.IGNORE) ? sm : SaveMethod.CREATE;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare party reference
        Validate.isTrue(entity.getPartyGroup() == null);
        entity.setPartyGroup((ParPartyGroup) partyInfo.getEntityRef(session));
        // prepare from reference
        Validate.isTrue(entity.getFrom() == null);
        if (validFromIdHolder != null) {
            entity.setFrom(validFromIdHolder.getEntityRef(session));
        }
        // prepare to reference
        Validate.isTrue(entity.getTo() == null);
        if (validToIdHolder != null) {
            entity.setTo(validToIdHolder.getEntityRef(session));
        }
    }

    @Override
    public void afterEntitySave(Session session) {
        // update party info
        partyInfo.onEntityPersist();
    }
}
