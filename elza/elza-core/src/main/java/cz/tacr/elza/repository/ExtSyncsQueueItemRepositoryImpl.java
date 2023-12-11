package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class ExtSyncsQueueItemRepositoryImpl implements ExtSyncsQueueItemRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ExtSyncsQueueItem> findExtSyncsQueueItemsByExternalSystemAndScopesAndState(String externalSystemCode,
                                                                                           List<ExtAsyncQueueState> states,
                                                                                           List<ApScope> scopes,
                                                                                           Integer firstResult,
                                                                                           Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ExtSyncsQueueItem> query = builder.createQuery(ExtSyncsQueueItem.class);
        Root<ExtSyncsQueueItem> from = query.from(ExtSyncsQueueItem.class);
        from.fetch(ExtSyncsQueueItem.ACCESS_POINT, JoinType.INNER);
        Join<ExtSyncsQueueItem, ApExternalSystem> externalSystemJoin = from.join(ExtSyncsQueueItem.EXTERNAL_SYSTEM,
                                                                                 JoinType.INNER);

        Predicate condition = builder.equal(externalSystemJoin.get(ApExternalSystem.CODE), externalSystemCode);
        // add state filter
        if (CollectionUtils.isNotEmpty(states)) {
            Predicate stateIn = from.get(ExtSyncsQueueItem.STATE).in(states);
            condition = builder.and(condition, stateIn);
        }
        // add scope filter
        if (CollectionUtils.isNotEmpty(scopes)) {
            Root<ApState> state = query.from(ApState.class);
            Join<ApState, ApAccessPoint> joinAp = state.join(ApState.FIELD_ACCESS_POINT, JoinType.INNER);
            Predicate accessPoint = builder.equal(joinAp.get(ApState.FIELD_ACCESS_POINT_ID), from.get(ApState.FIELD_ACCESS_POINT_ID));
            Predicate stateValid = state.get(ApState.FIELD_DELETE_CHANGE_ID).isNull();
            Predicate scopeIn = state.get(ApState.FIELD_SCOPE).in(scopes);
            condition = builder.and(condition, stateValid, accessPoint, scopeIn);
        }

        query.select(from);
        query.where(condition);
        query.orderBy(builder.desc(from.get(ExtSyncsQueueItem.DATE)));

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }
}
