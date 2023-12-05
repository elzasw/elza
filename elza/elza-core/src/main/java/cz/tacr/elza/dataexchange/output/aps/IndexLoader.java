package cz.tacr.elza.dataexchange.output.aps;

import java.util.Collections;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.groovy.GroovyResult;

public class IndexLoader extends AbstractEntityLoader<ApIndex, ApIndex> {

    private final ExportContext context;

    protected IndexLoader(ExportContext context, EntityManager em, int batchSize) {
        super(ApIndex.class, ApIndex.PART_ID, em, batchSize);
        this.context = context;
    }

    @Override
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> cq,
                                             Path<? extends ApIndex> root, CriteriaBuilder cb) {
        return root.get(ApIndex.INDEX_TYPE).in(Collections.singleton(GroovyResult.DISPLAY_NAME));
    }
}
