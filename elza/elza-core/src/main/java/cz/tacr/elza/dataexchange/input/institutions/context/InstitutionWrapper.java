package cz.tacr.elza.dataexchange.input.institutions.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;

public class InstitutionWrapper implements EntityWrapper {

    private final ParInstitution entity;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    public InstitutionWrapper(ParInstitution entity) {
        this.entity = Validate.notNull(entity);

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

    /**
     * TODO gotzy : přepracovat metodu bez getParty
     * @param session
     */
    @Override
    public void beforeEntitySave(Session session) {
        // prepare party reference
        //Validate.isTrue(entity.getParty() == null);
        //entity.setParty(partyInfo.getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
    }
}
