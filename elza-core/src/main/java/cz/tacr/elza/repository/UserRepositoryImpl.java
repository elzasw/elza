package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
import cz.tacr.elza.domain.UsrGroupUser;
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

    private <T> Predicate prepareFindUserByTextAndStateCount(final String search, final Boolean active, final Boolean disabled, final CriteriaBuilder builder, final Root<UsrUser> user, final CriteriaQuery<T> query) {
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
    public FilteredResult<UsrUser> findUserByTextAndStateCount(final String search, final Boolean active, final Boolean disabled, final Integer firstResult, final Integer maxResults, final Integer excludedGroupId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<UsrUser> query = builder.createQuery(UsrUser.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<UsrUser> user = query.from(UsrUser.class);
        Root<UsrUser> userCount = queryCount.from(UsrUser.class);

        Predicate condition = prepareFindUserByTextAndStateCount(search, active, disabled, builder, user, query);
        Predicate conditionCount = prepareFindUserByTextAndStateCount(search, active, disabled, builder, userCount, queryCount);

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

        if (excludedGroupId != null) {
            final Subquery<UsrUser> subquery = query.subquery(UsrUser.class);
            final Root<UsrGroupUser> groupUserSubq = subquery.from(UsrGroupUser.class);
            subquery.select(groupUserSubq.get(UsrGroupUser.USER_ID));
            subquery.where(builder.equal(groupUserSubq.get(UsrGroupUser.GROUP_ID), excludedGroupId));

            query.where(builder.not(builder.in(user.get(UsrUser.USER_ID)).value(subquery)));


            final Subquery<UsrUser> subqueryCount = queryCount.subquery(UsrUser.class);
            final Root<UsrGroupUser> groupUserCountSubq = subqueryCount.from(UsrGroupUser.class);
            subqueryCount.select(groupUserCountSubq.get(UsrGroupUser.USER_ID));
            subqueryCount.where(builder.equal(groupUserCountSubq.get(UsrGroupUser.GROUP_ID), excludedGroupId));

            queryCount.where(builder.not(builder.in(userCount.get(UsrUser.USER_ID)).value(subqueryCount)));
        }

        List<UsrUser> list = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
        long count = entityManager.createQuery(queryCount).getSingleResult();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }
}
