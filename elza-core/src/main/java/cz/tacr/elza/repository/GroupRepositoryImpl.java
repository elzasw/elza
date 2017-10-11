package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
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

    /**
     * Sestaví dotaz pro seznam kupin nebo jejich počet dle podmínek.
     * @param dataQuery pokud je true, vrátí se query se seznamem skupin, pokud je false, vrátí se query, které vrací počet skupin
     * @param search hledaný výraz
     * @param firstResult stránkování
     * @param maxResults stránkování, pokud je -1 neomezuje se
     * @return query
     */
    private TypedQuery buildGroupFindQuery(final boolean dataQuery, final String search, final Integer firstResult, final Integer maxResults) {
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
        q.setFirstResult(firstResult);
        if (maxResults >= 0) {
            q.setMaxResults(maxResults);
        }

        return q;
    }

    @Override
    public FilteredResult<UsrGroup> findGroupWithFundCreateByTextCount(final String search, final Integer firstResult, final Integer maxResults) {
        TypedQuery data = buildGroupFindQuery(true, search, firstResult, maxResults);
        TypedQuery count = buildGroupFindQuery(false, search, firstResult, maxResults);
        return new FilteredResult<>(firstResult, maxResults, ((Number) count.getSingleResult()).longValue(), data.getResultList());
    }
}
