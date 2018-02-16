package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrStructuredObject;

public class StructuredObjectLoader extends AbstractEntityLoader<Integer, ArrStructuredObject> {

    public StructuredObjectLoader(EntityManager em, int batchSize) {
        super(ArrStructuredObject.class, ArrStructuredObject.STRUCTURED_OBJECT_ID, em, batchSize);
    }
}
