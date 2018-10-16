package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;

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
		Join<UsrUser, ParParty> partyJoin = user.join(UsrUser.FIELD_PARTY, JoinType.INNER);
		Join<ParParty, ApAccessPoint> apJoin = partyJoin.join(ParParty.FIELD_RECORD, JoinType.INNER);
		Join<ApAccessPoint, ApName> nameJoin = ApAccessPointRepositoryImpl.preparePrefNameJoin(apJoin, builder);
		
		List<Predicate> conditions = new ArrayList<>();

		// Search
		if (StringUtils.isNotBlank(search)) {
			final String searchValue = "%" + search.toLowerCase() + "%";
			conditions.add(builder.or(
			        builder.like(builder.lower(nameJoin.get(ApName.FIELD_NAME)), searchValue),
			        builder.like(builder.lower(user.get(UsrUser.FIELD_USERNAME)), searchValue),
			        builder.like(builder.lower(user.get(UsrUser.FIELD_DESCRIPTION)), searchValue)));
		}

		if (excludedGroupId != null) {
			final Subquery<UsrUser> subquery = query.subquery(UsrUser.class);
			final Root<UsrGroupUser> groupUserSubq = subquery.from(UsrGroupUser.class);
			subquery.select(groupUserSubq.get(UsrGroupUser.FIELD_USER_ID));
			subquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.FIELD_GROUP_ID), excludedGroupId));
			conditions.add(builder.and(builder.not(builder.in(user.get(UsrUser.FIELD_USER_ID)).value(subquery))));
		}

		// Status if not all users
		if (!active || !disabled) {
			List<Predicate> statusConditions = new ArrayList<>();
			if (active) {
				statusConditions.add(builder.equal(user.get(UsrUser.FIELD_ACTIVE), true));
			}
			if (disabled) {
				statusConditions.add(builder.equal(user.get(UsrUser.FIELD_ACTIVE), false));
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

	/**
	 *
	 * @param search
	 * @param active
	 * @param disabled
	 * @param builder
	 * @param user
	 * @param excludedGroupId
	 * @param query
	 * @param userId
	 * @param includeUser Flag to include userId as one of queried users
	 * @return
	 */
    private <T> Predicate prepareFindUserByTextAndStateCount(
            final String search,
	        final boolean active,
	        final boolean disabled,
            final CriteriaBuilder builder,
            final Root<UsrUser> user,
            final Integer excludedGroupId,
            final CriteriaQuery<T> query,
	        final int userId,
	        final boolean includeUser) {
        Join<UsrUser, ParParty> partyJoin = user.join(UsrUser.FIELD_PARTY, JoinType.INNER);
        Join<ParParty, ApAccessPoint> apJoin = partyJoin.join(ParParty.FIELD_RECORD, JoinType.INNER);
        Join<ApAccessPoint, ApName> recordName = ApAccessPointRepositoryImpl.preparePrefNameJoin(apJoin, builder);
        
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(builder.or(
                    builder.like(builder.lower(recordName.get(ApName.FIELD_NAME)), searchValue),
                    builder.like(builder.lower(user.get(UsrUser.FIELD_USERNAME)), searchValue),
                    builder.like(builder.lower(user.get(UsrUser.FIELD_DESCRIPTION)), searchValue)
            ));
        }

        if (excludedGroupId != null) {
            final Subquery<UsrUser> subquery = query.subquery(UsrUser.class);
            final Root<UsrGroupUser> groupUserSubq = subquery.from(UsrGroupUser.class);
            subquery.select(groupUserSubq.get(UsrGroupUser.FIELD_USER_ID));
            subquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.FIELD_GROUP_ID), excludedGroupId));
            conditions.add(builder.and(builder.not(builder.in(user.get(UsrUser.FIELD_USER_ID)).value(subquery))));
        }

		// Innert query for userId
		// - see comment above with detail explanation
		final Subquery<Integer> subquery = query.subquery(Integer.class);
		final Root<UsrPermission> permissionUserSubq = subquery.from(UsrPermission.class);
		Join<UsrPermission, UsrGroup> ug = permissionUserSubq.join(UsrPermission.FIELD_GROUP_CONTROL, JoinType.LEFT);
		Join<UsrGroup, UsrGroupUser> ugu = ug.join(UsrGroup.FIELD_USERS, JoinType.LEFT);
		// outer group
		Join<UsrPermission, UsrGroup> ug3 = permissionUserSubq.join(UsrPermission.FIELD_GROUP, JoinType.LEFT);
		Join<UsrGroup, UsrGroupUser> ugu3 = ug3.join(UsrGroup.FIELD_USERS, JoinType.LEFT);

		subquery.where(
		        builder.and(
		                builder.or(
		                        builder.equal(permissionUserSubq.get(UsrPermission.FIELD_USER_ID), userId),
		                        builder.equal(ugu3.get(UsrGroupUser.FIELD_USER_ID), userId)
		                        ),
		                builder.in(permissionUserSubq.get(UsrPermission.FIELD_PERMISSION))
		                        .value(UsrPermission.Permission.USER_CONTROL_ENTITITY)
		                        .value(UsrPermission.Permission.GROUP_CONTROL_ENTITITY)
		                   )
		        );

		// prepare coalesce and select
		CriteriaBuilder.Coalesce<Integer> subQueryResult = builder.coalesce();
		subQueryResult.value(permissionUserSubq.get(UsrPermission.FIELD_USER_CONTROL_ID));
		subQueryResult.value(ugu.get(UsrGroupUser.FIELD_USER_ID));
		subquery.select(subQueryResult);


		// prepare collection of considered users
		Predicate userCondition;
		// add subquery as in condition
		In<Object> subqeryInPredicate = builder.in(user.get(UsrUser.FIELD_USER_ID)).value(subquery);
		if(includeUser) {
			userCondition = builder.or(
					subqeryInPredicate,
					builder.equal(user.get(UsrUser.FIELD_USER_ID), userId)
					);
		} else {
			userCondition = subqeryInPredicate;
		}
		conditions.add(userCondition);

		// Status if not all users
		if (!active || !disabled) {
			List<Predicate> statusConditions = new ArrayList<>();
			if (active) {
				statusConditions.add(builder.equal(user.get(UsrUser.FIELD_ACTIVE), true));
			}
			if (disabled) {
				statusConditions.add(builder.equal(user.get(UsrUser.FIELD_ACTIVE), false));
			}
			conditions.add(builder.or(statusConditions.toArray(new Predicate[statusConditions.size()])));
		}

		// Append conditions
        Predicate result = builder.and(conditions.toArray(new Predicate[conditions.size()]));

        return result;
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
			prepareUserView(user, builder, query);
			
			query.where(condition);
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
                                                               final int userId,
                                                               final boolean includeUser
                                                               ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrUser> query = builder.createQuery(UsrUser.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<UsrUser> user = query.from(UsrUser.class);
        Root<UsrUser> userCount = queryCount.from(UsrUser.class);

        Predicate condition = prepareFindUserByTextAndStateCount(search, active, disabled, builder, user,
                excludedGroupId, query, userId, includeUser);
        Predicate conditionCount = prepareFindUserByTextAndStateCount(search, active, disabled, builder, userCount,
                excludedGroupId, queryCount, userId, includeUser);

        query.select(user);
        queryCount.select(builder.countDistinct(userCount));

        if (condition != null) {
            prepareUserView(user, builder, query);
            
            query.where(condition);
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
    
    private static void prepareUserView(Root<UsrUser> user, CriteriaBuilder cb, CriteriaQuery<?> query) {
        Join<UsrUser, ParParty> partyJoin = user.join(UsrUser.FIELD_PARTY, JoinType.INNER);
        Join<ParParty, ApAccessPoint> apJoin = partyJoin.join(ParParty.FIELD_RECORD, JoinType.INNER);
        // join current preferred AP names
        Join<ApAccessPoint, ApName> nameJoin = ApAccessPointRepositoryImpl.preparePrefNameJoin(apJoin, cb);
        // define order
        Order order1 = cb.asc(nameJoin.get(ApName.FIELD_NAME));
        Order order2 = cb.asc(user.get(UsrUser.FIELD_USERNAME));
        query.orderBy(order1, order2);
    }
}
