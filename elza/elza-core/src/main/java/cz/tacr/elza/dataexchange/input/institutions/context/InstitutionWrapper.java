package cz.tacr.elza.dataexchange.input.institutions.context;

import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;

public class InstitutionWrapper implements EntityWrapper {

    private final ParInstitution entity;

    private final AccessPointInfo apInfo;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    public InstitutionWrapper(ParInstitution entity, AccessPointInfo apInfo) {
        this.entity = Validate.notNull(entity);
        this.apInfo = Validate.notNull(apInfo);
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

    /**     *
     * @param session
     */
    @Override
    public void beforeEntitySave(Session session) {
        Validate.isTrue(entity.getAccessPointId() == null);
        entity.setAccessPoint(apInfo.getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {
    }
}
