package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrAip;
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
                                                    final Root<ArrAip> aipRoot) {
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            Integer extId = null;
            try {
                extId = Integer.parseInt(search);
            } catch (Exception e) {

            }
            conditions.add(builder.or(
                    builder.equal(aipRoot.get(ArrAip.FIELD_EXT_AIP_ID), extId),
                    builder.like(builder.lower(aipRoot.get(ArrAip.FIELD_NAME)), searchValue)
            ));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public FilteredResult<ArrAip> findAips(final String search, final Integer firstResult, final Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ArrAip> query = builder.createQuery(ArrAip.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<ArrAip> aipRoot = query.from(ArrAip.class);
        Root<ArrAip> aipRootCount = queryCount.from(ArrAip.class);

        Predicate condition = prepareFindAipByTextCount(search, builder, aipRoot);
        Predicate conditionCount = prepareFindAipByTextCount(search, builder, aipRootCount);

        query.select(aipRoot);
        queryCount.select(builder.countDistinct(aipRootCount));

        if (condition != null) {
            Order order = builder.asc(aipRoot.get(ArrAip.FIELD_NAME));
            query.where(condition).orderBy(order);

            queryCount.where(conditionCount);
        }

        TypedQuery<ArrAip> tq = entityManager.createQuery(query)
                .setFirstResult(firstResult);
        if (maxResults > 0) {
            tq.setMaxResults(maxResults);
        }
        List<ArrAip> list = tq.getResultList();
		int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

}
