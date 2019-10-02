package cz.tacr.elza.dataexchange.output.aps;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApExternalId;

public class ExternalIdLoader extends AbstractEntityLoader<ApExternalId> {

    public ExternalIdLoader(EntityManager em, int batchSize) {
        super(ApExternalId.class, ApExternalId.ACCESS_POINT_ID, em, batchSize);
    }

    @Override
    protected Predicate createQueryCondition(Path<?> root, CriteriaBuilder cb) {
        return root.get(ApExternalId.DELETE_CHANGE_ID).isNull();
    }
}
