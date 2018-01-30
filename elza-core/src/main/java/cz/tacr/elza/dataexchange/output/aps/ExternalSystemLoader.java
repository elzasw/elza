package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.RegExternalSystem;

public class ExternalSystemLoader extends AbstractEntityLoader<RegExternalSystem> {

    public ExternalSystemLoader(EntityManager em, int batchSize) {
        super(RegExternalSystem.class, RegExternalSystem.PK, em, batchSize);
    }

}
