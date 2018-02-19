package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.domain.ArrDataStructureRef;

public class DelayedStructObjectRefWrapper extends ArrDataWrapper {

    private final String refStructObjectImportId;

    private final ContextSection section;

    DelayedStructObjectRefWrapper(ArrDataStructureRef entity, String refStructObjectImportId, ContextSection section) {
        super(entity);
        this.refStructObjectImportId = Validate.notNull(refStructObjectImportId);
        this.section = section;
    }

    public String getRefStructObjectImportId() {
        return refStructObjectImportId;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        ArrDataStructureRef ref = (ArrDataStructureRef) entity;
        Validate.isTrue(ref.getStructuredObject() == null);
        // init structured object reference
        ContextStructObject cso = section.getContextStructObject(refStructObjectImportId);
        ref.setStructuredObject(cso.getIdHolder().getEntityReference(session));
    }
}
