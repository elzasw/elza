package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Stánek
 * @since 15.06.2016
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
                    builder.like(builder.lower(group.get(UsrGroup.CODE)), searchValue),
                    builder.like(builder.lower(group.get(UsrGroup.NAME)), searchValue),
                    builder.like(builder.lower(group.get(UsrGroup.DESCRIPTION)), searchValue)
            ));
        }

        if (userId != null) {
            final Subquery<UsrGroup> subquery = query.subquery(UsrGroup.class);
            final Root<UsrPermission> permissionUserSubq = subquery.from(UsrPermission.class);
            subquery.select(permissionUserSubq.get(UsrPermission.GROUP_CONTROL_ID));

            final Subquery<UsrGroup> subsubquery = subquery.subquery(UsrGroup.class);
            final Root<UsrGroupUser> groupUserSubq = subsubquery.from(UsrGroupUser.class);
            subsubquery.select(groupUserSubq.get(UsrGroupUser.GROUP_ID));
            subsubquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.USER_ID), userId));

            subquery.where(builder.or(builder.equal(permissionUserSubq.get(UsrPermission.USER_ID), userId), builder.in(permissionUserSubq.get(UsrPermission.GROUP_ID)).value(subsubquery)));

            conditions.add(builder.and(builder.in(group.get(UsrGroup.GROUP_ID)).value(subquery)));
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

    /**
     * Sestaví dotaz pro seznam kupin nebo jejich počet dle podmínek.
     * @param dataQuery pokud je true, vrátí se query se seznamem skupin, pokud je false, vrátí se query, které vrací počet skupin
     * @param search hledaný výraz
     * @param firstResult stránkování
     * @param maxResults stránkování, pokud je -1 neomezuje se
     * @param userId identifikátor uživatele, podle kterého filtrujeme (pokud je null, nefiltrujeme)
     * @return query
     */
    private TypedQuery buildGroupFindQuery(final boolean dataQuery, final String search, final Integer firstResult,
                                           final Integer maxResults, final Integer userId) {
        StringBuilder conds = new StringBuilder();

        StringBuilder query = new StringBuilder();
        query.append("from usr_group g" +
                " left join usr_permission pu on pu.group = g"
        );

        query.append(" where pu.permission = :permission");

        // Podmínky hledání
        Map<String, Object> parameters = new HashMap<>();
        if (!StringUtils.isEmpty(search)) {
            conds.append(" and (lower(g.name) like :search or lower(g.code) like :search or lower(g.description) like :search)");
            parameters.put("search", "%" + search.toLowerCase() + "%");
        }

        // Připojení podmínek ke query
        if (conds.length() > 0) {
            query.append(conds.toString());
        }

        TypedQuery q;
        if (dataQuery) {
            String dataQueryStr = "select distinct g " + query.toString() + " order by g.name, g.groupId";
            q = entityManager.createQuery(dataQueryStr, UsrGroup.class);
        } else {
            String countQueryStr = "select count(distinct g) " + query.toString();
            q = entityManager.createQuery(countQueryStr, Number.class);
        }

        q.setParameter("permission", UsrPermission.Permission.FUND_CREATE);
        parameters.entrySet().forEach(e -> q.setParameter(e.getKey(), e.getValue()));

        if (dataQuery) {
            q.setFirstResult(firstResult);
            if (maxResults >= 0) {
                q.setMaxResults(maxResults);
            }
        }

        return q;
    }

    @Override
    public FilteredResult<UsrGroup> findGroupWithFundCreateByTextCount(final String search, final Integer firstResult, final Integer maxResults, final Integer userId) {
        TypedQuery data = buildGroupFindQuery(true, search, firstResult, maxResults, userId);
        TypedQuery count = buildGroupFindQuery(false, search, firstResult, maxResults, userId);
        return new FilteredResult<>(firstResult, maxResults, ((Number) count.getSingleResult()).longValue(), data.getResultList());
    }
}
