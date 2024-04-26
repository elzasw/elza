package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class AipRepositoryImpl implements AipRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    private <T> Predicate prepareFindAipByTextCount(final String search,
                                                    final CriteriaBuilder builder,
                                                    final Root<DaAip> aipRoot) {
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(
                    builder.like(builder.lower(aipRoot.get(DaAip.FIELD_CODE)), searchValue)
            );
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public FilteredResult<DaAip> findAips(final String search, final Integer firstResult, final Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<DaAip> query = builder.createQuery(DaAip.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<DaAip> aipRoot = query.from(DaAip.class);
        Root<DaAip> aipRootCount = queryCount.from(DaAip.class);

        Predicate condition = prepareFindAipByTextCount(search, builder, aipRoot);
        Predicate conditionCount = prepareFindAipByTextCount(search, builder, aipRootCount);

        query.select(aipRoot);
        queryCount.select(builder.countDistinct(aipRootCount));

        if (condition != null) {
            Order order = builder.asc(aipRoot.get(DaAip.FIELD_CODE));
            query.where(condition).orderBy(order);

            queryCount.where(conditionCount);
        }

        TypedQuery<DaAip> tq = entityManager.createQuery(query)
                .setFirstResult(firstResult);
        if (maxResults > 0) {
            tq.setMaxResults(maxResults);
        }
        List<DaAip> list = tq.getResultList();
		int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

}
