package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;

public class GroupRepositoryImpl implements GroupRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    private <T> Predicate prepareFindGroupByTextCount(final String search,
                                                      final CriteriaBuilder builder,
                                                      final Root<UsrGroup> usrGroupRoot,
                                                      final CriteriaQuery<T> criteriaQuery,
                                                      final Integer userId) {
        List<Predicate> conditions = new ArrayList<>();

        // Search
        if (StringUtils.isNotBlank(search)) {
            final String searchValue = "%" + search.toLowerCase() + "%";
            conditions.add(builder.or(
                    builder.like(builder.lower(usrGroupRoot.get(UsrGroup.FIELD_CODE)), searchValue),
                    builder.like(builder.lower(usrGroupRoot.get(UsrGroup.FIELD_NAME)), searchValue),
                    builder.like(builder.lower(usrGroupRoot.get(UsrGroup.FIELD_DESCRIPTION)), searchValue)
            ));
        }

        if (userId != null) {
        	// user musí být členem skupiny (UsrGroupUser) nebo mít opravneni skupinu spravovat (UsrPermission)

        	// select u.group_control_id from usr_permission u
            final Subquery<Integer> subquery = criteriaQuery.subquery(Integer.class);
            final Root<UsrPermission> usrPerminionRoot = subquery.from(UsrPermission.class);
            subquery.select(usrPerminionRoot.get(UsrPermission.FIELD_GROUP_CONTROL_ID));

            // select u.group_id from usr_group_user u where u.user_id = userId
            final Subquery<Integer> subsubquery = subquery.subquery(Integer.class);
            final Root<UsrGroupUser> usrGroupUserRoot = subsubquery.from(UsrGroupUser.class);
            subsubquery.select(usrGroupUserRoot.get(UsrGroupUser.FIELD_GROUP_ID));
            subsubquery.where(builder.equal(usrGroupUserRoot.get(UsrGroupUser.FIELD_USER_ID), userId));

            // select u.group_control_id from usr_permission u
            // where u.user_id = userId 
            //       or u.group_id in (select u.group_id from usr_group_user u where u.user_id = userId)
            subquery.where(builder.or(
                    builder.equal(usrPerminionRoot.get(UsrPermission.FIELD_USER_ID), userId), 
                    builder.in(usrPerminionRoot.get(UsrPermission.FIELD_GROUP_ID)).value(subsubquery)
            ));

            conditions.add(builder.or(
            		builder.in(usrGroupRoot.get(UsrGroup.FIELD_GROUP_ID)).value(subquery),
            		builder.in(usrGroupRoot.get(UsrGroup.FIELD_GROUP_ID)).value(subsubquery)
            ));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public FilteredResult<UsrGroup> findGroupByTextCount(final String search, final Integer firstResult, final Integer maxResults, final Integer userId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrGroup> criteriaQuery = builder.createQuery(UsrGroup.class);
        CriteriaQuery<Long> criteriaQueryCount = builder.createQuery(Long.class);

        Root<UsrGroup> group = criteriaQuery.from(UsrGroup.class);
        Root<UsrGroup> groupCount = criteriaQueryCount.from(UsrGroup.class);

        Predicate condition = prepareFindGroupByTextCount(search, builder, group, criteriaQuery, userId);
        Predicate conditionCount = prepareFindGroupByTextCount(search, builder, groupCount, criteriaQueryCount, userId);

        criteriaQuery.select(group);
        criteriaQueryCount.select(builder.countDistinct(groupCount));

        if (condition != null) {
            Order order = builder.asc(group.get(UsrGroup.FIELD_NAME));
            criteriaQuery.where(condition).orderBy(order);

            criteriaQueryCount.where(conditionCount);
        }

        TypedQuery<UsrGroup> tq = entityManager.createQuery(criteriaQuery).setFirstResult(firstResult);
        if (maxResults > 0) {
            tq.setMaxResults(maxResults);
        }
        List<UsrGroup> list = tq.getResultList();
		int count = entityManager.createQuery(criteriaQueryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

}
