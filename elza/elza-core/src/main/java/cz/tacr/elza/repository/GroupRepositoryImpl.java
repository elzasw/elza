package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;

/**
 */
public class GroupRepositoryImpl implements GroupRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    private <T> Predicate prepareFindGroupByTextCount(final String search,
                                                      final CriteriaBuilder builder,
                                                      final Root<UsrGroup> group,
                                                      final CriteriaQuery<T> query,
                                                      final Integer userId) {
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(builder.or(
                    builder.like(builder.lower(group.get(UsrGroup.FIELD_CODE)), searchValue),
                    builder.like(builder.lower(group.get(UsrGroup.FIELD_NAME)), searchValue),
                    builder.like(builder.lower(group.get(UsrGroup.FIELD_DESCRIPTION)), searchValue)
            ));
        }

        if (userId != null) {
            final Subquery<UsrGroup> subquery = query.subquery(UsrGroup.class);
            final Root<UsrPermission> permissionUserSubq = subquery.from(UsrPermission.class);
            subquery.select(permissionUserSubq.get(UsrPermission.FIELD_GROUP_CONTROL_ID));

            final Subquery<UsrGroup> subsubquery = subquery.subquery(UsrGroup.class);
            final Root<UsrGroupUser> groupUserSubq = subsubquery.from(UsrGroupUser.class);
            subsubquery.select(groupUserSubq.get(UsrGroupUser.FIELD_GROUP_ID));
            subsubquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.FIELD_USER_ID), userId));

            subquery.where(builder.or(builder.equal(permissionUserSubq.get(UsrPermission.FIELD_USER_ID), userId), builder.in(permissionUserSubq.get(UsrPermission.FIELD_GROUP_ID)).value(subsubquery)));

            conditions.add(builder.and(builder.in(group.get(UsrGroup.FIELD_GROUP_ID)).value(subquery)));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public FilteredResult<UsrGroup> findGroupByTextCount(final String search, final Integer firstResult, final Integer maxResults, final Integer userId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrGroup> query = builder.createQuery(UsrGroup.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<UsrGroup> group = query.from(UsrGroup.class);
        Root<UsrGroup> groupCount = queryCount.from(UsrGroup.class);

        Predicate condition = prepareFindGroupByTextCount(search, builder, group, query, userId);
        Predicate conditionCount = prepareFindGroupByTextCount(search, builder, groupCount, queryCount, userId);

        query.select(group);
        queryCount.select(builder.countDistinct(groupCount));

        if (condition != null) {
            Order order = builder.asc(group.get(UsrGroup.FIELD_NAME));
            query.where(condition).orderBy(order);

            queryCount.where(conditionCount);
        }

        List<UsrGroup> list = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
		int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

}
