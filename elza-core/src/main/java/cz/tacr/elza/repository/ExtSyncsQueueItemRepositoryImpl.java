package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.ExtAsyncQueueState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.List;

public class ExtSyncsQueueItemRepositoryImpl implements ExtSyncsQueueItemRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ExtSyncsQueueItem> findExtSyncsQueueItemsByExternalSystemAndScopesAndState(String externalSystemCode, List<ExtAsyncQueueState> states, List<String> scopes, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ExtSyncsQueueItem> query = builder.createQuery(ExtSyncsQueueItem.class);
        Root<ExtSyncsQueueItem> extSyncsQueueItemRoot = query.from(ExtSyncsQueueItem.class);
        extSyncsQueueItemRoot.fetch(ExtSyncsQueueItem.ACCESS_POINT, JoinType.INNER);
        Join<ExtSyncsQueueItem, ApExternalSystem> externalSystemJoin = extSyncsQueueItemRoot.join(ExtSyncsQueueItem.EXTERNAL_SYSTEM, JoinType.INNER);

        Predicate condition = builder.equal(externalSystemJoin.get(ApExternalSystem.CODE), externalSystemCode);

        query.select(extSyncsQueueItemRoot);
        query.where(condition);

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }
}
