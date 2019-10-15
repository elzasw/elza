package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class PartyNameCmplWrapper implements EntityWrapper {

    private final ParPartyNameComplement entity;

    private final EntityIdHolder<ParPartyName> nameIdHolder;

    private final PartyInfo partyInfo;

    PartyNameCmplWrapper(ParPartyNameComplement entity, EntityIdHolder<ParPartyName> nameIdHolder,
            PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.nameIdHolder = Validate.notNull(nameIdHolder);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    @Override
    public SaveMethod getSaveMethod() {
        SaveMethod sm = partyInfo.getSaveMethod();
        // party name complement is never updated and old must be invalidate by storage
        return sm.equals(SaveMethod.IGNORE) ? sm : SaveMethod.CREATE;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare name reference
        Validate.isTrue(entity.getPartyName() == null);
        entity.setPartyName(nameIdHolder.getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
        // update party info
        partyInfo.onEntityPersist();
    }
}
