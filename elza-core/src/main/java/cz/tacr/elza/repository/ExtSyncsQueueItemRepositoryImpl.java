package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class ExtSyncsQueueItemRepositoryImpl implements ExtSyncsQueueItemRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ExtSyncsQueueItem> findExtSyncsQueueItemsByExternalSystemAndScopesAndState(String externalSystemCode,
                                                                                           List<ExtAsyncQueueState> states,
                                                                                           List<String> scopes,
                                                                                           Integer firstResult,
                                                                                           Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ExtSyncsQueueItem> query = builder.createQuery(ExtSyncsQueueItem.class);
        Root<ExtSyncsQueueItem> from = query.from(ExtSyncsQueueItem.class);
        from.fetch(ExtSyncsQueueItem.ACCESS_POINT, JoinType.INNER);
        Join<ExtSyncsQueueItem, ApExternalSystem> externalSystemJoin = from.join(ExtSyncsQueueItem.EXTERNAL_SYSTEM,
                                                                                 JoinType.INNER);

        Predicate condition = builder.equal(externalSystemJoin.get(ApExternalSystem.CODE), externalSystemCode);
        // Add state filter
        if (CollectionUtils.isNotEmpty(states)) {
            Predicate stateIn = from.get(ExtSyncsQueueItem.STATE).in(states);
            condition = builder.and(condition, stateIn);
        }
        // TODO: Add scope filter

        query.select(from);
        query.where(condition);
        query.orderBy(builder.desc(from.get(ExtSyncsQueueItem.DATE)));

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }
}
