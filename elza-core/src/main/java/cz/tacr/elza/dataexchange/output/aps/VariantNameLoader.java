package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.RegVariantRecord;

public class VariantNameLoader extends AbstractEntityLoader<Integer, RegVariantRecord> {

    public VariantNameLoader(EntityManager em, int batchSize) {
        super(RegVariantRecord.class, RegVariantRecord.RECORD_FK, em, batchSize);
    }
}
