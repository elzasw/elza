package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApBindingState;

public class ExternalIdLoader extends AbstractEntityLoader<ApBindingState, ApBindingState> {

    public ExternalIdLoader(EntityManager em, int batchSize) {
        super(ApBindingState.class, ApBindingState.ACCESS_POINT_ID, em, batchSize);
    }

    @Override
    protected Predicate createQueryCondition(Path<? extends ApBindingState> root, CriteriaBuilder cb) {
        return root.get(ApBindingState.DELETE_CHANGE_ID).isNull();
    }
}
