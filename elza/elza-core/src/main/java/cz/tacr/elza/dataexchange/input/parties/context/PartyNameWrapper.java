package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyNameWrapper implements EntityWrapper {

    private final SimpleIdHolder<ParPartyName> idHolder = new SimpleIdHolder<>(ParPartyName.class, false);

    private final ParPartyName entity;

    private final PartyInfo partyInfo;

    private EntityIdHolder<ParUnitdate> validFromIdHolder;

    private EntityIdHolder<ParUnitdate> validToIdHolder;

    PartyNameWrapper(ParPartyName entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    public EntityIdHolder<ParPartyName> getIdHolder() {
        return idHolder;
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
        // party name is never updated and old must be invalidate by storage
        return sm.equals(SaveMethod.IGNORE) ? sm : SaveMethod.CREATE;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare party reference
        Validate.isTrue(entity.getParty() == null);
        entity.setParty(partyInfo.getEntityRef(session));
        // prepare from reference
        Validate.isTrue(entity.getValidFrom() == null);
        if (validFromIdHolder != null) {
            entity.setValidFrom(validFromIdHolder.getEntityRef(session));
        }
        // prepare to reference
        Validate.isTrue(entity.getValidTo() == null);
        if (validToIdHolder != null) {
            entity.setValidTo(validToIdHolder.getEntityRef(session));
        }
    }

    @Override
    public void afterEntitySave(Session session) {
        // init id holder
        idHolder.setEntityId(entity.getPartyNameId());
        // update party info
        partyInfo.onEntityPersist();
    }
}
