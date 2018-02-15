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

import cz.tacr.elza.domain.*;
import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.ApRecord;

/**
 * Rozšířené repository pro uživatele.
 *
 */
//@Component
public class UserRepositoryImpl implements UserRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

	private <T> Predicate prepareFindUserByText(
	        final String search,
	        final boolean active,
	        final boolean disabled,
	        final CriteriaBuilder builder,
	        final Root<UsrUser> user,
	        final Integer excludedGroupId,
	        final CriteriaQuery<T> query) {
		Join<UsrUser, ParParty> party = user.join(UsrUser.PARTY, JoinType.INNER);
		Join<ParParty, ApRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
		List<Predicate> conditions = new ArrayList<>();

		// Search
		if (StringUtils.isNotBlank(search)) {
			final String searchValue = "%" + search.toLowerCase() + "%";
			conditions.add(builder.or(
			        builder.like(builder.lower(record.get(ApRecord.RECORD)), searchValue),
			        builder.like(builder.lower(user.get(UsrUser.USERNAME)), searchValue),
			        builder.like(builder.lower(user.get(UsrUser.DESCRIPTION)), searchValue)));
		}

		if (excludedGroupId != null) {
			final Subquery<UsrUser> subquery = query.subquery(UsrUser.class);
			final Root<UsrGroupUser> groupUserSubq = subquery.from(UsrGroupUser.class);
			subquery.select(groupUserSubq.get(UsrGroupUser.USER_ID));
			subquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.GROUP_ID), excludedGroupId));
			conditions.add(builder.and(builder.not(builder.in(user.get(UsrUser.USER_ID)).value(subquery))));
		}

		// Status if not all users
		if (!active || !disabled) {
			List<Predicate> statusConditions = new ArrayList<>();
			if (active) {
				statusConditions.add(builder.equal(user.get(UsrUser.ACTIVE), true));
			}
			if (disabled) {
				statusConditions.add(builder.equal(user.get(UsrUser.ACTIVE), false));
			}
			conditions.add(builder.or(statusConditions.toArray(new Predicate[statusConditions.size()])));
		}

		return builder.and(conditions.toArray(new Predicate[conditions.size()]));
	}

	/*
	 Inner condition to select users controlled directly or indirectly by the given user:

	 0. First idea (unions not supported by JPA)
	 select user_control_id from usr_permission up
	 where up.permission = 'USER_CONTROL_ENTITITY'
	 union
	 select ugu.user_id from usr_permission up
	 join usr_group_user ugu on ugu.group_id = up.group_control_id
	 where up.permission = 'GROUP_CONTROL_ENTITITY'


	 1. Select users controlled by this user:

	 select * from usr_permission p
	 left join usr_group g on p.group_control_id  = g.group_id
	 left join usr_group_user gu on g.group_id = gu.group_id
	 where  gu.user_id = 22 and p.permission in ('USER_CONTROL_ENTITITY' , 'GROUP_CONTROL_ENTITITY'  )

	 2. Select users controlled by group in which this user is member:

	 select coalesce(p.user_control_id, gu.user_id) from usr_permission p
	 left join usr_group g on p.group_control_id = g.group_id
	 left join usr_group_user gu on gu.group_id = g.group_id
	 join usr_group g3 on p.group_id = g3.group_id
	 join usr_group_user gu3 on g3.group_id = gu3.group_id
	 where  gu3.user_id = 22 and p.permission in ('USER_CONTROL_ENTITITY' , 'GROUP_CONTROL_ENTITITY'  )

	 3. Final query to select users controlled by this user directly or indirectly:

	 select distinct coalesce(p.user_control_id, gu.user_id) from usr_permission p
	 left join usr_group g on p.group_control_id = g.group_id
	 left join usr_group_user gu on gu.group_id = g.group_id
	 -- add outer group
	 left join usr_group g3 on p.group_id = g3.group_id
	 left join usr_group_user gu3 on g3.group_id = gu3.group_id
	 where  (p.user_id = 22 or gu3.user_id = 22) and p.permission in ('USER_CONTROL_ENTITITY' , 'GROUP_CONTROL_ENTITITY'  )

	 */

    private <T> Predicate prepareFindUserByTextAndStateCount(
            final String search,
	        final boolean active,
	        final boolean disabled,
            final CriteriaBuilder builder,
            final Root<UsrUser> user,
            final Integer excludedGroupId,
            final CriteriaQuery<T> query,
	        final int userId) {
        Join<UsrUser, ParParty> party = user.join(UsrUser.PARTY, JoinType.INNER);
        Join<ParParty, ApRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(builder.or(
                    builder.like(builder.lower(record.get(ApRecord.RECORD)), searchValue),
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

		// Innert query for userId
		// - see comment above with detail explanation
		final Subquery<Integer> subquery = query.subquery(Integer.class);
		final Root<UsrPermission> permissionUserSubq = subquery.from(UsrPermission.class);
		Join<UsrPermission, UsrGroup> ug = permissionUserSubq.join(UsrPermission.GROUP_CONTROL, JoinType.LEFT);
		Join<UsrGroup, UsrGroupUser> ugu = ug.join(UsrGroup.USERS, JoinType.LEFT);
		// outer group
		Join<UsrPermission, UsrGroup> ug3 = permissionUserSubq.join(UsrPermission.GROUP, JoinType.LEFT);
		Join<UsrGroup, UsrGroupUser> ugu3 = ug3.join(UsrGroup.USERS, JoinType.LEFT);

		subquery.where(
		        builder.and(
		                builder.or(
		                        builder.equal(permissionUserSubq.get(UsrPermission.USER_ID), userId),
		                        builder.equal(ugu3.get(UsrGroupUser.USER_ID), userId)),
		                builder.in(permissionUserSubq.get(UsrPermission.PERMISSION))
		                        .value(UsrPermission.Permission.USER_CONTROL_ENTITITY)
		                        .value(UsrPermission.Permission.GROUP_CONTROL_ENTITITY)));

		// prepare coalesce and select
		CriteriaBuilder.Coalesce<Integer> subQueryResult = builder.coalesce();
		subQueryResult.value(permissionUserSubq.get(UsrPermission.USER_CONTROL_ID));
		subQueryResult.value(ugu.get(UsrGroupUser.USER_ID));
		subquery.select(subQueryResult);

		// add subquery as in condition
		conditions.add(
		        builder.and(
		                builder.in(user.get(UsrUser.USER_ID)).value(subquery)));

		// Status if not all users
		if (!active || !disabled) {
			List<Predicate> statusConditions = new ArrayList<>();
			if (active) {
				statusConditions.add(builder.equal(user.get(UsrUser.ACTIVE), true));
			}
			if (disabled) {
				statusConditions.add(builder.equal(user.get(UsrUser.ACTIVE), false));
			}
			conditions.add(builder.or(statusConditions.toArray(new Predicate[statusConditions.size()])));
		}

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

	@Override
	public FilteredResult<UsrUser> findUserByText(String search, boolean active, boolean disabled,
	        int firstResult, int maxResults, Integer excludedGroupId) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<UsrUser> query = builder.createQuery(UsrUser.class);
		CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

		Root<UsrUser> user = query.from(UsrUser.class);
		Root<UsrUser> userCount = queryCount.from(UsrUser.class);

		Predicate condition = prepareFindUserByText(search, active, disabled, builder, user, excludedGroupId, query);
		Predicate conditionCount = prepareFindUserByText(search, active, disabled, builder, userCount, excludedGroupId,
		        queryCount);

		query.select(user);
		queryCount.select(builder.countDistinct(userCount));

		if (condition != null) {
			Join<UsrUser, ParParty> party = user.join(UsrUser.PARTY, JoinType.INNER);
			Join<ParParty, ApRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
			Order order1 = builder.asc(record.get(ApRecord.RECORD));
			Order order2 = builder.asc(user.get(UsrUser.USERNAME));
			query.where(condition).orderBy(order1, order2);

			queryCount.where(conditionCount);
		}

		List<UsrUser> list = entityManager.createQuery(query)
		        .setFirstResult(firstResult)
		        .setMaxResults(maxResults)
		        .getResultList();
		int count = list.size();
		// count number of items
		if (count >= maxResults || firstResult != 0) {
			count = entityManager.createQuery(queryCount).getSingleResult().intValue();
		}

		return new FilteredResult<>(firstResult, maxResults, count, list);
	}

    @Override
    public FilteredResult<UsrUser> findUserByTextAndStateCount(final String search,
	        final boolean active,
	        final boolean disabled,
                                                               final int firstResult,
                                                               final int maxResults,
                                                               final Integer excludedGroupId,
                                                               final int userId) {
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
            Join<ParParty, ApRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
            Order order1 = builder.asc(record.get(ApRecord.RECORD));
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
