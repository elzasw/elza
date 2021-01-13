package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrDao;

public class DaoLoader extends AbstractEntityLoader<ArrDao, ArrDao> {

    protected DaoLoader(EntityManager em, int batchSize) {
        super(ArrDao.class, ArrDao.FIELD_DAO_ID, em, batchSize);
    }

}
