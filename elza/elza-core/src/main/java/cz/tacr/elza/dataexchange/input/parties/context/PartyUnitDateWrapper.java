package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParUnitdate;

public class PartyUnitDateWrapper implements EntityWrapper {

    private final SimpleIdHolder<ParUnitdate> idHolder = new SimpleIdHolder<>(ParUnitdate.class, false);

    private final ParUnitdate entity;

    private final PartyInfo partyInfo;

    PartyUnitDateWrapper(ParUnitdate entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    public EntityIdHolder<ParUnitdate> getIdHolder() {
        return idHolder;
    }

    @Override
    public SaveMethod getSaveMethod() {
        SaveMethod sm = partyInfo.getSaveMethod();
        // unit date is never updated and old must be invalidate by storage
        return sm.equals(SaveMethod.IGNORE) ? sm : SaveMethod.CREATE;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // NOP   
    }

    @Override
    public void afterEntitySave(Session session) {
        // init id holder
        idHolder.setEntityId(entity.getUnitdateId());
        // update party info
        partyInfo.onEntityPersist();
    }
}
