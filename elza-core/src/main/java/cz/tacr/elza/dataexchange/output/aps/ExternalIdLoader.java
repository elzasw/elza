package cz.tacr.elza.dataexchange.output.aps;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApBindingState;

public class ExternalIdLoader extends AbstractEntityLoader<ApBindingState, ApBindingState> {

    public ExternalIdLoader(EntityManager em, int batchSize) {
        super(ApBindingState.class, ApBindingState.ACCESS_POINT_ID, em, batchSize);
    }

    @Override
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> cq,
                                             Path<? extends ApBindingState> root, CriteriaBuilder cb) {
        return root.get(ApBindingState.DELETE_CHANGE_ID).isNull();
    }
}
