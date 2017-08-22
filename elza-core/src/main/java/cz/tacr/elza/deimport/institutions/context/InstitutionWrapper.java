package cz.tacr.elza.deimport.institutions.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;

public class InstitutionWrapper implements EntityWrapper {

    private final ParInstitution entity;

    private final PartyImportInfo partyInfo;

    public InstitutionWrapper(ParInstitution entity, PartyImportInfo partyInfo) {
        this.entity = Objects.requireNonNull(entity);
        this.partyInfo = Objects.requireNonNull(partyInfo);
    }

    @Override
    public boolean isCreate() {
        return isNotIgnored() && entity.getInstitutionId() == null;
    }

    @Override
    public boolean isUpdate() {
        return isNotIgnored() && entity.getInstitutionId() != null;
    }

    @Override
    public ParInstitution getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        entity.setParty(partyInfo.getEntityRef(session, ParParty.class));
    }

    private boolean isNotIgnored() {
        return !partyInfo.getState().equals(State.IGNORE);
    }
}
