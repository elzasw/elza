package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
import cz.tacr.elza.deimport.context.SimpleStatefulIdHolder;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyNameWrapper implements EntityWrapper, EntityMetrics {

    private final ParPartyName entity;

    private final PartyImportInfo partyInfo;

    private final SimpleStatefulIdHolder idHolder;

    private StatefulIdHolder validFromIdHolder;

    private StatefulIdHolder validToIdHolder;

    PartyNameWrapper(ParPartyName entity, PartyImportInfo partyInfo) {
        this.entity = Objects.requireNonNull(entity);
        this.partyInfo = Objects.requireNonNull(partyInfo);
        this.idHolder = new SimpleStatefulIdHolder(ParPartyName.class, partyInfo);
    }

    public StatefulIdHolder getIdHolder() {
        return idHolder;
    }

    public void setValidFrom(StatefulIdHolder validFromIdHolder) {
        this.validFromIdHolder = validFromIdHolder;
    }

    public void setValidTo(StatefulIdHolder validToIdHolder) {
        this.validToIdHolder = validToIdHolder;
    }

    @Override
    public boolean isCreate() {
        return !idHolder.getState().equals(State.IGNORE);
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public ParPartyName getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // party relation
        Assert.isNull(entity.getParty());
        entity.setParty(partyInfo.getEntityRef(session, ParParty.class));
        // valid from relation
        Assert.isNull(entity.getValidFrom());
        if (validFromIdHolder != null) {
            entity.setValidFrom(validFromIdHolder.getEntityRef(session, ParUnitdate.class));
        }
        // valid to relation
        Assert.isNull(entity.getValidTo());
        if (validToIdHolder != null) {
            entity.setValidTo(validToIdHolder.getEntityRef(session, ParUnitdate.class));
        }
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getPartyNameId());
    }
}
