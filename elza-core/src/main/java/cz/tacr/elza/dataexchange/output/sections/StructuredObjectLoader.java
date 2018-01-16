package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrStructureData;

public class StructuredObjectLoader extends AbstractEntityLoader<Integer, ArrStructureData> {

    public StructuredObjectLoader(EntityManager em, int batchSize) {
        super(ArrStructureData.class, ArrStructureData.STRUCTURE_DATA_ID, em, batchSize);
    }
}
