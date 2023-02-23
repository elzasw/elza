package cz.tacr.elza.dataexchange.output.aps;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApItem;

public class ItemLoader extends AbstractEntityLoader<ApItem, ApItem> {

    private final ExportContext context;

    protected ItemLoader(ExportContext context, EntityManager em, int batchSize) {
        super(ApItem.class, ApItem.PART_ID, em, batchSize);
        this.context = context;
    }

    @Override
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> cq,
                                             Path<? extends ApItem> root, CriteriaBuilder cb) {
        return root.get(ApItem.DELETE_CHANGE_ID).isNull();
    }
}
