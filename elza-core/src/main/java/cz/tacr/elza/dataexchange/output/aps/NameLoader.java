package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApName;

public class NameLoader extends AbstractEntityLoader<ApName, ApName> {

    public NameLoader(EntityManager em, int batchSize) {
        super(ApName.class, ApName.FIELD_ACCESS_POINT_ID, em, batchSize);
    }

    @Override
    protected Predicate createQueryCondition(Path<? extends ApName> root, CriteriaBuilder cb) {
        return root.get(ApName.FIELD_DELETE_CHANGE_ID).isNull();
    }
}
