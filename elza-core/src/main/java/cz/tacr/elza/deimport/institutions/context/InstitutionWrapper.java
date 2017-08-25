package cz.tacr.elza.deimport.institutions.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;

public class InstitutionWrapper implements EntityWrapper {

    private final ParInstitution entity;

    private final PartyImportInfo partyInfo;

    public InstitutionWrapper(ParInstitution entity, PartyImportInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    @Override
    public EntityState getState() {
        switch (partyInfo.getState()) {
            case IGNORE:
                return EntityState.IGNORE;
            case CREATE:
            case UPDATE:
                return entity.getInstitutionId() != null ? EntityState.UPDATE : EntityState.CREATE;
            default:
                throw new IllegalStateException("Invalid entity state:" + partyInfo.getState());
        }
    }

    @Override
    public ParInstitution getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        entity.setParty(partyInfo.getEntityRef(session, ParParty.class));
    }
}
