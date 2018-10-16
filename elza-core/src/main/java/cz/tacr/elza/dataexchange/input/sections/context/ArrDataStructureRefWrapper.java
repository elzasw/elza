package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.domain.ArrDataStructureRef;

/**
 * Wrapper for referenced structured object which does not exist during
 * item-data processing.
 */
public class ArrDataStructureRefWrapper extends ArrDataWrapper {

    private final ArrDataStructureRef dataStuctRef;

    private final String structObjImportId;

    private final SectionContext sectionCtx;

    ArrDataStructureRefWrapper(ArrDataStructureRef entity, String structObjImportId, SectionContext sectionCtx) {
        super(entity);
        this.dataStuctRef = Validate.notNull(entity);
        this.structObjImportId = Validate.notNull(structObjImportId);
        this.sectionCtx = sectionCtx;
    }

    public String getStructObjImportId() {
        return structObjImportId;
    }

    @Override
    public void beforeEntitySave(Session session) {
        // prepare structured object reference
        Validate.isTrue(dataStuctRef.getStructuredObject() == null);
        StructObjContext structObjCtx = sectionCtx.getStructObject(structObjImportId);
        dataStuctRef.setStructuredObject(structObjCtx.getIdHolder().getEntityRef(session));
        // call super
        super.beforeEntitySave(session);
    }
}
