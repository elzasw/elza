package cz.tacr.elza.dataexchange.input.institutions.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParInstitution;

public class InstitutionWrapper implements EntityWrapper {

    private final ParInstitution entity;

    private final PartyInfo partyInfo;

    public InstitutionWrapper(ParInstitution entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    @Override
    public PersistMethod getPersistMethod() {
        PersistMethod persistMethod = partyInfo.getPersistMethod();
        switch (persistMethod) {
            case NONE:
                return persistMethod;
            case CREATE:
            case UPDATE:
                return entity.getInstitutionId() != null ? PersistMethod.UPDATE : PersistMethod.CREATE;
            default:
                throw new IllegalStateException("Invalid persist method:" + persistMethod);
        }
    }

    @Override
    public ParInstitution getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity.getParty() == null);
        entity.setParty(partyInfo.getEntityRef(session));
    }
}
