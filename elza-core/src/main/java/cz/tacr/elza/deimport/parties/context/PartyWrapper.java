package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
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
        this.entity = Objects.requireNonNull(entity);
        this.info = Objects.requireNonNull(info);
    }

    public PartyImportInfo getInfo() {
        return info;
    }

    @Override
    public boolean isCreate() {
        return info.getState().equals(State.CREATE);
    }

    @Override
    public boolean isUpdate() {
        return info.getState().equals(State.UPDATE);
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
        Assert.isNull(entity.getRecord());
        entity.setRecord(info.getRecordRef(session));
    }

    @Override
    public void afterEntityPersist() {
        info.init(entity.getPartyId());
    }
}
