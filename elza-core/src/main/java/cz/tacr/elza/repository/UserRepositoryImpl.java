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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;

/**
 * Rozšířené repository pro uživatele.
 *
 * @author Pavel Stánek
 * @since 15.06.2016
 */
//@Component
public class UserRepositoryImpl implements UserRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    private <T> Predicate prepareFindUserByTextAndStateCount(
            final String search,
            final Boolean active,
            final Boolean disabled,
            final CriteriaBuilder builder,
            final Root<UsrUser> user,
            final Integer excludedGroupId,
            final CriteriaQuery<T> query,
            final Integer userId) {
        Join<UsrUser, ParParty> party = user.join(UsrUser.PARTY, JoinType.INNER);
        Join<ParParty, RegRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchValue),
                    builder.like(builder.lower(user.get(UsrUser.USERNAME)), searchValue),
                    builder.like(builder.lower(user.get(UsrUser.DESCRIPTION)), searchValue)
            ));
        }

        if (excludedGroupId != null) {
            final Subquery<UsrUser> subquery = query.subquery(UsrUser.class);
            final Root<UsrGroupUser> groupUserSubq = subquery.from(UsrGroupUser.class);
            subquery.select(groupUserSubq.get(UsrGroupUser.USER_ID));
            subquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.GROUP_ID), excludedGroupId));
            conditions.add(builder.and(builder.not(builder.in(user.get(UsrUser.USER_ID)).value(subquery))));
        }

        if (userId != null) {
            final Subquery<UsrUser> subquery = query.subquery(UsrUser.class);
            final Root<UsrPermission> permissionUserSubq = subquery.from(UsrPermission.class);
            subquery.select(permissionUserSubq.get(UsrPermission.USER_CONTROL_ID));

            final Subquery<UsrGroup> subsubquery = subquery.subquery(UsrGroup.class);
            final Root<UsrGroupUser> groupUserSubq = subsubquery.from(UsrGroupUser.class);
            subsubquery.select(groupUserSubq.get(UsrGroupUser.GROUP_ID));
            subsubquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.USER_ID), userId));

            subquery.where(builder.or(builder.equal(permissionUserSubq.get(UsrPermission.USER_ID), userId), builder.in(permissionUserSubq.get(UsrPermission.GROUP_ID)).value(subsubquery)));

            conditions.add(builder.and(builder.in(user.get(UsrUser.USER_ID)).value(subquery)));
        }

        // Status
        List<Predicate> statusConditions = new ArrayList<>();
        if (active) {
            statusConditions.add(builder.equal(user.get(UsrUser.ACTIVE), true));
        }
        if (disabled) {
            statusConditions.add(builder.equal(user.get(UsrUser.ACTIVE), false));
        }
        conditions.add(builder.or(statusConditions.toArray(new Predicate[statusConditions.size()])));

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public FilteredResult<UsrUser> findUserByTextAndStateCount(final String search,
                                                               final Boolean active,
                                                               final Boolean disabled,
                                                               final Integer firstResult,
                                                               final Integer maxResults,
                                                               final Integer excludedGroupId,
                                                               final Integer userId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrUser> query = builder.createQuery(UsrUser.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<UsrUser> user = query.from(UsrUser.class);
        Root<UsrUser> userCount = queryCount.from(UsrUser.class);

        Predicate condition = prepareFindUserByTextAndStateCount(search, active, disabled, builder, user, excludedGroupId, query, userId);
        Predicate conditionCount = prepareFindUserByTextAndStateCount(search, active, disabled, builder, userCount, excludedGroupId, queryCount, userId);

        query.select(user);
        queryCount.select(builder.countDistinct(userCount));

        if (condition != null) {
            Join<UsrUser, ParParty> party = user.join(UsrUser.PARTY, JoinType.INNER);
            Join<ParParty, RegRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
            Order order1 = builder.asc(record.get(RegRecord.RECORD));
            Order order2 = builder.asc(user.get(UsrUser.USERNAME));
            query.where(condition).orderBy(order1, order2);

            queryCount.where(conditionCount);
        }

        List<UsrUser> list = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
		int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

    /**
     * Sestaví dotaz pro seznam uživatelů nebo jejich počet dle podmínek.
     * @param dataQuery pokud je true, vrátí se query se seznamem uživatelů, pokud je false, vrátí se query, které vrací počet uživatelů
     * @param search hledaný výraz
     * @param active ve výstupu mají být aktivní uživatelé?
     * @param disabled ve výstupu mají být neaktivní uživatelé?
     * @param firstResult stránkování
     * @param maxResults stránkování, pokud je -1 neomezuje se
     * @param excludedGroupId z jaké skupiny uživatele nechceme
     * @param userId identifikátor uživatele, podle kterého filtrujeme (pokud je null, nefiltrujeme)
     * @return query
     */
    private TypedQuery buildUserFindQuery(final boolean dataQuery, final String search, final Boolean active, final Boolean disabled, final Integer firstResult, final Integer maxResults, final Integer excludedGroupId, final Integer userId) {
        StringBuilder conds = new StringBuilder();

        StringBuilder query = new StringBuilder();
        query.append("from usr_user u" +
                " left join usr_permission pu on pu.user = u" +
                " left join usr_group_user gu on gu.user = u" +
                " left join usr_permission pg on pg.group = gu.group"
        );

        // Pro datový dotaz potřebujeme fetch kvůli optimatlizaci a kvůli řazení dle record (musí být v selectu a to zajistí fetch)
        if (dataQuery) {
            query.append(" inner join fetch u.party party" +
                    " inner join fetch party.record record");
        } else {
            query.append(" inner join u.party party" +
                    " inner join party.record record");
        }
        query.append(" where (pu.permission = :permission or pg.permission = :permission)");

        // Podmínky hledání
        Map<String, Object> parameters = new HashMap<>();
        if (!StringUtils.isEmpty(search)) {
            conds.append(" and (lower(u.username) like :search or lower(u.description) like :search or lower(record.record) like :search)");
            parameters.put("search", "%" + search.toLowerCase() + "%");
        }

        if (userId != null) {
            conds.append(" AND u.userId IN (SELECT p.userControlId FROM usr_permission p WHERE p.userId = :userId OR p.groupId IN (SELECT gu.groupId FROM usr_group_user gu WHERE gu.userId = :userId))");
            parameters.put("userId", userId);
        }

        StringBuilder status = new StringBuilder();
        if (active) {
            status.append("u.active = :active");
            parameters.put("active", true);
        }
        if (disabled) {
            if (status.length() > 0) {
                status.append(" or ");
            }
            status.append("u.active = :disabled");
            parameters.put("disabled", false);
        }
        if (status.length() > 0) {
            conds.append(" and ");
            conds.append(status.toString());
        }

        // Exclude group id
        if (excludedGroupId != null) {
            conds.append(" and u.userId not in (select groupId.userId from usr_group_user rel where rel.groupId = :groupId)");
            parameters.put("groupId", excludedGroupId);
        }

        // Připojení podmínek ke query
        if (conds.length() > 0) {
            query.append(conds.toString());
        }

        TypedQuery q;
        if (dataQuery) {
            String dataQueryStr = "select distinct u " + query.toString() + " order by u.username, record.record, u.userId";
            q = entityManager.createQuery(dataQueryStr, UsrUser.class);
        } else {
            String countQueryStr = "select count(distinct u) " + query.toString();
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
    public FilteredResult<UsrUser> findUserWithFundCreateByTextAndStateCount(final String search, final Boolean active, final Boolean disabled, final Integer firstResult, final Integer maxResults, final Integer excludedGroupId, final Integer userId) {
        TypedQuery data = buildUserFindQuery(true, search, active, disabled, firstResult, maxResults, excludedGroupId, userId);
        TypedQuery count = buildUserFindQuery(false, search, active, disabled, firstResult, maxResults, excludedGroupId, userId);
		return new FilteredResult<>(firstResult, maxResults, ((Number) count.getSingleResult()).intValue(),
		        data.getResultList());
    }
}
