package cz.tacr.elza.dataexchange.input.institutions.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;

public class InstitutionWrapper implements EntityWrapper {

    private final ParInstitution entity;

    private final PartyInfo partyInfo;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    public InstitutionWrapper(ParInstitution entity, PartyInfo partyInfo) {
        this.entity = Validate.notNull(entity);
        this.partyInfo = Validate.notNull(partyInfo);
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public SaveMethod getSaveMethod() {
        return saveMethod;
    }

    void setSaveMethod(SaveMethod saveMethod) {
        this.saveMethod = saveMethod;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare party reference
        Validate.isTrue(entity.getParty() == null);
        entity.setParty(partyInfo.getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
    }
}
