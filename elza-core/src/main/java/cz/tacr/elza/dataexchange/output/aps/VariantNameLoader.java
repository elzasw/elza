package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApName;

public class VariantNameLoader extends AbstractEntityLoader<ApName> {

    public VariantNameLoader(EntityManager em, int batchSize) {
        super(ApName.class, ApName.ACCESS_POINT_ID, em, batchSize);
    }
}
