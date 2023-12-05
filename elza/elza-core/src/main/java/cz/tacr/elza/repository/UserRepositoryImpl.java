package cz.tacr.elza.repository;

import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.*;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import java.util.ArrayList;
import java.util.List;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME_LOWER;

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
	        final CriteriaQuery<T> query,
            final SearchType searchTypeName,
            final SearchType searchTypeUsername) {

		List<Predicate> conditions = new ArrayList<>();

        Path<String> accessPointName = null;
        Path<String> userName = null;

        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            Join<UsrUser, ApAccessPoint> apJoin = user.join(UsrUser.FIELD_ACCESS_POINT, JoinType.INNER);
            Predicate apFkCond = builder.equal(user.get(UsrUser.FIELD_ACCESS_POINT), apJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID));
            Join<ApAccessPoint, ApPart> nameJoin = apJoin.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.INNER);
            Predicate nameFkCond = builder.equal(apJoin.get(ApAccessPoint.FIELD_PREFFERED_PART),
                    nameJoin.get(ApPart.PART_ID));
            nameJoin.on(nameFkCond);
            Join<ApIndex, ApPart> indexJoin = nameJoin.join(ApPart.INDICES, JoinType.INNER);
            indexJoin.on(builder.equal(indexJoin.get(ApIndex.INDEX_TYPE), DISPLAY_NAME_LOWER));
            accessPointName = indexJoin.get(ApIndex.VALUE);
        }

        Predicate nameLikeCond = null;
        Predicate usernameLikeCond = null;

        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            String searchNameExp = org.apache.commons.lang.StringUtils.trimToNull(search);
            if (searchNameExp != null) {
                switch (searchTypeName) {
                    case FULLTEXT:
                        searchNameExp = '%' + searchNameExp.toLowerCase() + '%';
                        break;
                    case RIGHT_SIDE_LIKE:
                        searchNameExp = searchNameExp.toLowerCase() + '%';
                        break;
                    default:
                        break;
                }
                // add description join
                // Join<ApAccessPoint, ApDescription> descJoin = apJoin.join(ApAccessPoint.DESCRIPTIONS, JoinType.LEFT);
                // Predicate descFkCond = cb.equal(apJoin.get(ApAccessPoint.ACCESS_POINT_ID), descJoin.get(ApDescription.ACCESS_POINT_ID));
                // Predicate activeDescCond = descJoin.get(ApDescription.DELETE_CHANGE_ID).isNull();
                // descJoin.on(cb.and(descFkCond, activeDescCond));

                // add like condition to where
                nameLikeCond = builder.like(builder.lower(accessPointName), searchNameExp);
            }
        }

		// Search
        if (searchTypeUsername == SearchType.FULLTEXT || searchTypeUsername == SearchType.RIGHT_SIDE_LIKE) {
            String searchUsernameExp = org.apache.commons.lang.StringUtils.trimToNull(search);
            if (searchUsernameExp != null) {
                switch (searchTypeUsername) {
                    case FULLTEXT:
                        searchUsernameExp = '%' + searchUsernameExp.toLowerCase() + '%';
                        break;
                    case RIGHT_SIDE_LIKE:
                        searchUsernameExp = searchUsernameExp.toLowerCase() + '%';
                        break;
                    default:
                        break;
                }
                usernameLikeCond = builder.or(
                        builder.like(builder.lower(user.get(UsrUser.FIELD_USERNAME)), searchUsernameExp),
                        builder.like(builder.lower(user.get(UsrUser.FIELD_DESCRIPTION)), searchUsernameExp));
            }
        }
        if(nameLikeCond != null && usernameLikeCond == null) conditions.add(nameLikeCond);
        if(nameLikeCond == null && usernameLikeCond != null) conditions.add(usernameLikeCond);
        if(nameLikeCond != null && usernameLikeCond != null) {
            conditions.add(builder.or(nameLikeCond, usernameLikeCond));
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
	        final boolean includeUser,
            @Nullable SearchType searchTypeName,
            @Nullable SearchType searchTypeUsername) {

        searchTypeName = searchTypeName != null ? searchTypeName : SearchType.DISABLED;
        searchTypeUsername = searchTypeUsername != null ? searchTypeUsername : SearchType.FULLTEXT;

        List<Predicate> conditions = new ArrayList<>();

        Path<String> accessPointName = null;
        Path<String> userName = null;

        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            Join<UsrUser, ApAccessPoint> apJoin = user.join(UsrUser.FIELD_ACCESS_POINT, JoinType.INNER);
            Predicate apFkCond = builder.equal(user.get(UsrUser.FIELD_ACCESS_POINT), apJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID));
            Join<ApAccessPoint, ApPart> nameJoin = apJoin.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.INNER);
            Predicate nameFkCond = builder.equal(apJoin.get(ApAccessPoint.FIELD_PREFFERED_PART),
                    nameJoin.get(ApPart.PART_ID));
            nameJoin.on(nameFkCond);
            Join<ApIndex, ApPart> indexJoin = nameJoin.join(ApPart.INDICES, JoinType.INNER);
            indexJoin.on(builder.equal(indexJoin.get(ApIndex.INDEX_TYPE), DISPLAY_NAME_LOWER));
            accessPointName = indexJoin.get(ApIndex.VALUE);
        }

        Predicate nameLikeCond = null;
        Predicate usernameLikeCond = null;

        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            String searchNameExp = org.apache.commons.lang.StringUtils.trimToNull(search);
            if (searchNameExp != null) {
                switch (searchTypeName) {
                    case FULLTEXT:
                        searchNameExp = '%' + searchNameExp.toLowerCase() + '%';
                        break;
                    case RIGHT_SIDE_LIKE:
                        searchNameExp = searchNameExp.toLowerCase() + '%';
                        break;
                    default:
                        break;
                }
                // add description join
                // Join<ApAccessPoint, ApDescription> descJoin = apJoin.join(ApAccessPoint.DESCRIPTIONS, JoinType.LEFT);
                // Predicate descFkCond = cb.equal(apJoin.get(ApAccessPoint.ACCESS_POINT_ID), descJoin.get(ApDescription.ACCESS_POINT_ID));
                // Predicate activeDescCond = descJoin.get(ApDescription.DELETE_CHANGE_ID).isNull();
                // descJoin.on(cb.and(descFkCond, activeDescCond));

                // add like condition to where
                nameLikeCond = builder.like(builder.lower(accessPointName), searchNameExp);
            }
        }

        // Search
        if (searchTypeUsername == SearchType.FULLTEXT || searchTypeUsername == SearchType.RIGHT_SIDE_LIKE) {
            String searchUsernameExp = org.apache.commons.lang.StringUtils.trimToNull(search);
            if (searchUsernameExp != null) {
                switch (searchTypeUsername) {
                    case FULLTEXT:
                        searchUsernameExp = '%' + searchUsernameExp.toLowerCase() + '%';
                        break;
                    case RIGHT_SIDE_LIKE:
                        searchUsernameExp = searchUsernameExp.toLowerCase() + '%';
                        break;
                    default:
                        break;
                }
                usernameLikeCond = builder.or(
                        builder.like(builder.lower(user.get(UsrUser.FIELD_USERNAME)), searchUsernameExp),
                        builder.like(builder.lower(user.get(UsrUser.FIELD_DESCRIPTION)), searchUsernameExp));
            }
        }
        if(nameLikeCond != null && usernameLikeCond == null) conditions.add(nameLikeCond);
        if(nameLikeCond == null && usernameLikeCond != null) conditions.add(usernameLikeCond);
        if(nameLikeCond != null && usernameLikeCond != null) {
            conditions.add(builder.or(nameLikeCond, usernameLikeCond));
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
                                                  int firstResult, int maxResults, Integer excludedGroupId, SearchType searchTypeName, SearchType searchTypeUsername ) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<UsrUser> query = builder.createQuery(UsrUser.class);
		CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

		Root<UsrUser> user = query.from(UsrUser.class);
		Root<UsrUser> userCount = queryCount.from(UsrUser.class);

		Predicate condition = prepareFindUserByText(search, active, disabled, builder, user, excludedGroupId, query, searchTypeName, searchTypeUsername);
		Predicate conditionCount = prepareFindUserByText(search, active, disabled, builder, userCount, excludedGroupId,
		        queryCount, searchTypeName, searchTypeUsername);

		query.select(user);
		queryCount.select(builder.countDistinct(userCount));

		if (condition != null) {
			prepareUserView(user, builder, query);

			query.where(condition);
			queryCount.where(conditionCount);
		}

		TypedQuery<UsrUser> tq = entityManager.createQuery(query)
		    .setFirstResult(firstResult);
		if(maxResults>0) {
            tq.setMaxResults(maxResults);
		}
		List<UsrUser> list = tq.getResultList();
		int count = list.size();
		// count number of items
        if ((maxResults > 0 && count >= maxResults) || firstResult != 0) {
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
                                                               final boolean includeUser, SearchType searchTypeName, SearchType searchTypeUsername
                                                               ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrUser> query = builder.createQuery(UsrUser.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<UsrUser> user = query.from(UsrUser.class);
        Root<UsrUser> userCount = queryCount.from(UsrUser.class);

        Predicate condition = prepareFindUserByTextAndStateCount(search, active, disabled, builder, user,
                excludedGroupId, query, userId, includeUser, searchTypeName, searchTypeUsername);
        Predicate conditionCount = prepareFindUserByTextAndStateCount(search, active, disabled, builder, userCount,
                excludedGroupId, queryCount, userId, includeUser, searchTypeName, searchTypeUsername);

        query.select(user);
        queryCount.select(builder.countDistinct(userCount));

        if (condition != null) {
            prepareUserView(user, builder, query);

            query.where(condition);
            queryCount.where(conditionCount);
        }

        TypedQuery<UsrUser> tq = entityManager.createQuery(query)
                .setFirstResult(firstResult);
        if (maxResults > 0) {
            tq.setMaxResults(maxResults);
        }
        List<UsrUser> list = tq.getResultList();
        int count = list.size();
        // count number of items
        if ((maxResults > 0 && count >= maxResults) || firstResult != 0) {
            count = entityManager.createQuery(queryCount).getSingleResult().intValue();
        }

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

    private static void prepareUserView(Root<UsrUser> user, CriteriaBuilder cb, CriteriaQuery<?> query) {
        // join current preferred AP names

        // define order
        Order order2 = cb.asc(user.get(UsrUser.FIELD_USERNAME));
        query.orderBy(order2);
    }
}
