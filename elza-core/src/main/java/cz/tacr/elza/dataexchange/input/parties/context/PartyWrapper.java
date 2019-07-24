package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParParty;

public class PartyWrapper implements EntityWrapper {

    private final ParParty entity;

    private final PartyInfo partyInfo;

    PartyWrapper(ParParty entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    public PartyInfo getPartyInfo() {
        return partyInfo;
    }

    @Override
    public SaveMethod getSaveMethod() {
        return partyInfo.getSaveMethod();
    }

    @Override
    public ParParty getEntity() {
        return entity;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare AP reference
        Validate.isTrue(entity.getAccessPoint() == null);
        entity.setAccessPoint(partyInfo.getApInfo().getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
        // update party info
        partyInfo.setEntityId(entity.getPartyId());
        partyInfo.onEntityPersist();
    }
}
