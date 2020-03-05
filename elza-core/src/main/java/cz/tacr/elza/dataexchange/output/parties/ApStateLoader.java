package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApState;

public class ApStateLoader extends AbstractEntityLoader<ApState, ApState> {

    public ApStateLoader(EntityManager em, int batchSize) {
        super(ApState.class, ApState.FIELD_ACCESS_POINT_ID, em, batchSize);
    }

    @Override
    protected void buildExtendedQuery(Root<? extends ApState> baseEntity, CriteriaBuilder cb) {
        baseEntity.fetch(ApState.FIELD_ACCESS_POINT);
    }

    @Override
    protected Predicate createQueryCondition(Path<? extends ApState> root, CriteriaBuilder cb) {
        return root.get(ApState.FIELD_DELETE_CHANGE_ID).isNull();
    }
}
