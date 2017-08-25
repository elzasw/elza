package cz.tacr.elza.deimport.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.SimpleStatefulIdHolder;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
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
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
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
    public EntityState getState() {
        return idHolder.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.CREATE;
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
        Validate.isTrue(entity.getParty() == null);
        entity.setParty(partyInfo.getEntityRef(session, ParParty.class));
        // valid from relation
        Validate.isTrue(entity.getValidFrom() == null);
        if (validFromIdHolder != null) {
            entity.setValidFrom(validFromIdHolder.getEntityRef(session, ParUnitdate.class));
        }
        // valid to relation
        Validate.isTrue(entity.getValidTo() == null);
        if (validToIdHolder != null) {
            entity.setValidTo(validToIdHolder.getEntityRef(session, ParUnitdate.class));
        }
    }

    @Override
    public void afterEntityPersist() {
        idHolder.setId(entity.getPartyNameId());
    }
}
