package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApBinding;

public class ExternalIdLoader extends AbstractEntityLoader<ApBinding, ApBinding> {

    public ExternalIdLoader(EntityManager em, int batchSize) {
        super(ApBinding.class, ApBinding.ACCESS_POINT_ID, em, batchSize);
    }

    @Override
    protected Predicate createQueryCondition(Path<? extends ApBinding> root, CriteriaBuilder cb) {
        return root.get(ApBinding.DELETE_CHANGE_ID).isNull();
    }
}
