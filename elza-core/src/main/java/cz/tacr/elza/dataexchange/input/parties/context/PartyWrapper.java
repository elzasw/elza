package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParParty;

/**
 * Created by todtj on 12.06.2017.
 */
public class PartyWrapper implements EntityWrapper, EntityMetrics {

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
    public PersistMethod getPersistMethod() {
        return partyInfo.getPersistMethod();
    }

    @Override
    public ParParty getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getRecord() == null);
        entity.setRecord(partyInfo.getAPReference(session));
    }

    @Override
    public void afterEntityPersist() {
        partyInfo.setEntityId(entity.getPartyId());
    }
}
