package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApExternalSystem;

public class ExternalSystemLoader extends AbstractEntityLoader<Integer, ApExternalSystem> {

    public ExternalSystemLoader(EntityManager em, int batchSize) {
        super(ApExternalSystem.class, ApExternalSystem.PK, em, batchSize);
    }

}
