package cz.tacr.elza.repository;

import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Implementace respozitory pro aprecord.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class ApAccessPointRepositoryImpl implements ApAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ApState> findApAccessPointByTextAndType(
            @Nullable String searchRecord,
            @Nullable Collection<Integer> apTypeIds,
            Integer firstResult,
            Integer maxResults,
            @Nullable Set<Integer> scopeIds,
            @Nullable Collection<ApState.StateApproval> approvalStates,
            @Nullable SearchType searchTypeName,
            @Nullable SearchType searchTypeUsername) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ApState> query = builder.createQuery(ApState.class);
        Root<ApAccessPoint> accessPointRoot = query.from(ApAccessPoint.class);
        Root<ApState> apStateRoot = query.from(ApState.class);

        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ApAccessPoint> accessPointSubquery = subquery.from(ApAccessPoint.class);
        Root<ApState> apStateSubquery = subquery.from(ApState.class);

        Predicate condition = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, approvalStates, accessPointRoot, apStateRoot, builder, query::orderBy, true, searchTypeName, searchTypeUsername);
        Predicate conditionSubquery = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, approvalStates, accessPointSubquery, apStateSubquery, builder, null, false, searchTypeName, searchTypeUsername);

        subquery.select(accessPointSubquery.get(ApAccessPoint.FIELD_ACCESS_POINT_ID));
        subquery.where(conditionSubquery);

        query.select(apStateRoot);
        if (condition != null) {
            query.where(condition, builder.in(accessPointRoot.get(ApAccessPoint.FIELD_ACCESS_POINT_ID)).value(subquery));
        }
        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findApAccessPointByTextAndTypeCount(
            @Nullable String searchRecord,
            @Nullable Collection<Integer> apTypeIds,
            @Nullable Set<Integer> scopeIds,
            @Nullable Collection<ApState.StateApproval> approvalStates,
            @Nullable SearchType searchTypeName,
            @Nullable SearchType searchTypeUsername) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return 0L;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ApAccessPoint> accessPoint = query.from(ApAccessPoint.class);
        Root<ApState> apState = query.from(ApState.class);

        Predicate condition = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, approvalStates, accessPoint, apState, builder, null, false, searchTypeName, searchTypeUsername);

        query.select(builder.countDistinct(accessPoint));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Prepares predicate for AP search. Only necessary entities will be joined.
     *
     * @param searchValue if present APs with partial match in name or description are returned
     * @param apTypeIds   if not empty APs with same type are returned
     * @param scopeIds    APs with same scope are returned, cannot be empty
     * @param accessPoint query root or entity join of AP
     * @param cb          JPA query builder
     * @return AP predicate which can be used as where condition.
     */
    public static Predicate prepareApSearchPredicate(@Nullable final String searchValue,
                                                     @Nullable final Collection<Integer> apTypeIds,
                                                     final Collection<Integer> scopeIds,
                                                     @Nullable final Collection<ApState.StateApproval> approvalStates,
                                                     final From<?, ApAccessPoint> accessPoint,
                                                     final From<?, ApState> apState,
                                                     final CriteriaBuilder cb,
                                                     final Consumer<Order> accessPointNameCallback,
                                                     final boolean onlyPrefferedName,
                                                     @Nullable SearchType searchTypeName,
                                                     @Nullable SearchType searchTypeUsername) {
        searchTypeName = searchTypeName != null ? searchTypeName : SearchType.FULLTEXT;
        searchTypeUsername = searchTypeUsername != null ? searchTypeUsername : SearchType.DISABLED;

        // prepare conjunction list
        List<Predicate> conjunctions = new ArrayList<>();

        conjunctions.add(cb.equal(apState.get(ApState.FIELD_ACCESS_POINT_ID), accessPoint.get(ApAccessPoint.FIELD_ACCESS_POINT_ID)));

        // search only active AP
        conjunctions.add(apState.get(ApState.FIELD_DELETE_CHANGE_ID).isNull());
        conjunctions.add(cb.or(accessPoint.get(ApAccessPoint.STATE).isNull(), cb.notEqual(accessPoint.get(ApAccessPoint.STATE), ApStateEnum.TEMP)));

        // add name join
        Path<String> accessPointName = null;
        Path<String> userName = null;
        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            Join<ApAccessPoint, ApPart> nameJoin = accessPoint.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.LEFT);
            Predicate nameFkCond = cb.equal(accessPoint.get(ApAccessPoint.FIELD_PREFFERED_PART),
                    nameJoin.get(ApPart.PART_ID));
            Predicate activeNameCond = nameJoin.get(ApPart.DELETE_CHANGE_ID).isNull();
            nameJoin.on(cb.and(nameFkCond, activeNameCond));
            accessPointName = nameJoin.get(ApPart.VALUE);
            if (accessPointNameCallback != null) {
                accessPointNameCallback.accept(cb.asc(accessPointName));
            }
        }

        if (searchTypeUsername == SearchType.JOIN) {
            Join<ApAccessPoint, UsrUser> userJoin = accessPoint.join(ApAccessPoint.FIELD_USER_LIST, JoinType.INNER);
            Predicate userFkCond = cb.equal(accessPoint.get(ApAccessPoint.FIELD_ACCESS_POINT_ID),
                    userJoin.get(UsrUser.FIELD_ACCESS_POINT));
            Predicate activeUserCond = cb.equal(userJoin.get(UsrUser.FIELD_ACTIVE), true);
            userJoin.on(cb.and(userFkCond, activeUserCond));

        } else if (searchTypeUsername == SearchType.FULLTEXT || searchTypeUsername == SearchType.RIGHT_SIDE_LIKE) {
            Join<ApAccessPoint, UsrUser> userJoin = accessPoint.join(ApAccessPoint.FIELD_USER_LIST, JoinType.INNER);
            Predicate userFkCond = cb.equal(accessPoint.get(ApAccessPoint.FIELD_ACCESS_POINT_ID),
                    userJoin.get(UsrUser.FIELD_ACCESS_POINT));
            Predicate activeUserCond = cb.equal(userJoin.get(UsrUser.FIELD_ACTIVE), true);
            userJoin.on(cb.or(userFkCond, activeUserCond));

            userName = userJoin.get(UsrUser.FIELD_USERNAME);
            if (accessPointNameCallback != null) {
                accessPointNameCallback.accept(cb.asc(userName));
            }
        }

        // add text search
        Predicate nameLikeCond = null;
        Predicate usernameLikeCond = null;
        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            String searchNameExp = StringUtils.trimToNull(searchValue);
            if (searchTypeName == SearchType.DISABLED || searchTypeName == SearchType.JOIN) {
                searchNameExp = "";
            }
            if (searchNameExp != null && !onlyPrefferedName) {
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
                nameLikeCond = cb.like(cb.lower(accessPointName), searchNameExp);
            }
        }

        if (searchTypeUsername == SearchType.FULLTEXT || searchTypeUsername == SearchType.RIGHT_SIDE_LIKE) {
            String searchUsernameExp = StringUtils.trimToNull(searchValue);
            if (searchUsernameExp != null && !onlyPrefferedName) {
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
                usernameLikeCond = cb.like(cb.lower(userName), searchUsernameExp);
            }
        }
        if(nameLikeCond != null && usernameLikeCond == null) conjunctions.add(nameLikeCond);
        if(nameLikeCond == null && usernameLikeCond != null) conjunctions.add(usernameLikeCond);
        if(nameLikeCond != null && usernameLikeCond != null) {
            conjunctions.add(cb.or(nameLikeCond, usernameLikeCond));
        }

        // add scope id condition
        Validate.isTrue(!scopeIds.isEmpty());
        conjunctions.add(apState.get(ApState.FIELD_SCOPE_ID).in(scopeIds));

        // add AP type condition
        if (CollectionUtils.isNotEmpty(apTypeIds)) {
            conjunctions.add(apState.get(ApState.FIELD_AP_TYPE_ID).in(apTypeIds));
        }

        if (CollectionUtils.isNotEmpty(approvalStates)) {
            conjunctions.add(apState.get(ApState.FIELD_STATE_APPROVAL).in(approvalStates));
        }

        return cb.and(conjunctions.toArray(new Predicate[0]));
    }
}
