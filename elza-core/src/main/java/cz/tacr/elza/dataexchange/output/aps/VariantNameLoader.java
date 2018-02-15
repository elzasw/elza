package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApVariantRecord;

public class VariantNameLoader extends AbstractEntityLoader<Integer, ApVariantRecord> {

    public VariantNameLoader(EntityManager em, int batchSize) {
        super(ApVariantRecord.class, ApVariantRecord.RECORD_FK, em, batchSize);
    }
}
