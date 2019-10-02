package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ParUnitdate;

public class UnitdateLoader extends AbstractEntityLoader<ParUnitdate> {

    public UnitdateLoader(EntityManager em, int batchSize) {
        super(ParUnitdate.class, ParUnitdate.FIELD_UNITDATE_ID, em, batchSize);
    }
}
