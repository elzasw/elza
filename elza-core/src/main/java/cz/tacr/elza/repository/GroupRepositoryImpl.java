package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public class GroupRepositoryImpl implements GroupRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    private <T> Predicate prepareFindGroupByTextCount(final String search, final CriteriaBuilder builder, final Root<UsrGroup> group, final CriteriaQuery<T> query) {
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(builder.or(
                    builder.like(builder.lower(group.get(UsrGroup.CODE)), searchValue),
                    builder.like(builder.lower(group.get(UsrGroup.NAME)), searchValue),
                    builder.like(builder.lower(group.get(UsrGroup.DESCRIPTION)), searchValue)
            ));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public FilteredResult<UsrGroup> findGroupByTextCount(final String search, final Integer firstResult, final Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrGroup> query = builder.createQuery(UsrGroup.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<UsrGroup> group = query.from(UsrGroup.class);
        Root<UsrGroup> groupCount = queryCount.from(UsrGroup.class);

        Predicate condition = prepareFindGroupByTextCount(search, builder, group, query);
        Predicate conditionCount = prepareFindGroupByTextCount(search, builder, groupCount, queryCount);

        query.select(group);
        queryCount.select(builder.countDistinct(groupCount));

        if (condition != null) {
            Order order = builder.asc(group.get(UsrGroup.NAME));
            query.where(condition).orderBy(order);

            queryCount.where(conditionCount);
        }

        List<UsrGroup> list = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
        long count = entityManager.createQuery(queryCount).getSingleResult();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }
}
