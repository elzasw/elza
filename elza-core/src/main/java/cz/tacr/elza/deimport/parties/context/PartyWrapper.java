package cz.tacr.elza.deimport.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParParty;

/**
 * Created by todtj on 12.06.2017.
 */
public class PartyWrapper implements EntityWrapper, EntityMetrics {

    private final ParParty entity;

    private final PartyImportInfo info;

    PartyWrapper(ParParty entity, PartyImportInfo info) {
        this.entity = Validate.notNull(entity);
        this.info = Validate.notNull(info);
    }

    public PartyImportInfo getInfo() {
        return info;
    }

    @Override
    public EntityState getState() {
        return info.getState();
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
        entity.setRecord(info.getRecordRef(session));
    }

    @Override
    public void afterEntityPersist() {
        info.init(entity.getPartyId());
    }
}
