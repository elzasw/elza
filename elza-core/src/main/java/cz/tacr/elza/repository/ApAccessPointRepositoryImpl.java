package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;

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
            @Nullable Collection<ApState.StateApproval> approvalStates) {

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

        Predicate condition = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, approvalStates, accessPointRoot, apStateRoot, builder, query::orderBy, true);
        Predicate conditionSubquery = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, approvalStates, accessPointSubquery, apStateSubquery, builder, null, false);

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
            @Nullable Collection<ApState.StateApproval> approvalStates) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return 0L;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ApAccessPoint> accessPoint = query.from(ApAccessPoint.class);
        Root<ApState> apState = query.from(ApState.class);

        Predicate condition = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, approvalStates, accessPoint, apState, builder, null, false);

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
     * @param apTypeIds if not empty APs with same type are returned
     * @param scopeIds APs with same scope are returned, cannot be empty
     * @param accessPoint query root or entity join of AP
     * @param cb JPA query builder
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
                                                     final boolean onlyPrefferedName) {
        // prepare conjunction list
        List<Predicate> conjunctions = new ArrayList<>();

        conjunctions.add(cb.equal(apState.get(ApState.FIELD_ACCESS_POINT_ID), accessPoint.get(ApAccessPoint.FIELD_ACCESS_POINT_ID)));

        // search only active AP
        conjunctions.add(apState.get(ApState.FIELD_DELETE_CHANGE_ID).isNull());
        conjunctions.add(cb.or(accessPoint.get(ApAccessPoint.STATE).isNull(), cb.notEqual(accessPoint.get(ApAccessPoint.STATE), ApStateEnum.TEMP)));

        // add name join
        Join<ApAccessPoint, ApName> nameJoin = accessPoint.join(ApAccessPoint.FIELD_NAMES, JoinType.LEFT);
        Predicate nameFkCond = cb.equal(accessPoint.get(ApAccessPoint.FIELD_ACCESS_POINT_ID),
                nameJoin.get(ApName.FIELD_ACCESS_POINT_ID));
        Predicate activeNameCond = nameJoin.get(ApName.FIELD_DELETE_CHANGE_ID).isNull();
        nameJoin.on(cb.and(nameFkCond, activeNameCond));

        Path<String> accessPointName = nameJoin.get(ApName.FIELD_NAME);

        if (accessPointNameCallback != null) {
            accessPointNameCallback.accept(cb.asc(accessPointName));
        }

        if (onlyPrefferedName) {
            conjunctions.add(cb.isTrue(nameJoin.get(ApName.FIELD_PREFERRED_NAME)));
        }

        // add text search
        String searchExp = StringUtils.trimToNull(searchValue);
        if (searchExp != null && !onlyPrefferedName) {
            searchExp = '%' + searchExp.toLowerCase() + '%';

            // add description join
            // Join<ApAccessPoint, ApDescription> descJoin = apJoin.join(ApAccessPoint.DESCRIPTIONS, JoinType.LEFT);
            // Predicate descFkCond = cb.equal(apJoin.get(ApAccessPoint.ACCESS_POINT_ID), descJoin.get(ApDescription.ACCESS_POINT_ID));
            // Predicate activeDescCond = descJoin.get(ApDescription.DELETE_CHANGE_ID).isNull();
            // descJoin.on(cb.and(descFkCond, activeDescCond));

            // add like condition to where
            Predicate nameLikeCond = cb.like(cb.lower(accessPointName), searchExp);
            conjunctions.add(nameLikeCond);
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

    /**
     * Prepares inner join for preferred AP name (preferred name is always expected).
     *
     * @param fromAp query root or entity join of AP
     * @param cb JPA criteria builder
     * @return Created preferred AP name join.
     */
    public static Join<ApAccessPoint, ApName> preparePrefNameJoin(From<?, ApAccessPoint> fromAp, CriteriaBuilder cb) {
        Join<ApAccessPoint, ApName> join = fromAp.join(ApAccessPoint.FIELD_NAMES, JoinType.INNER);
        Predicate fkCond = cb.equal(fromAp.get(ApAccessPoint.FIELD_ACCESS_POINT_ID), join.get(ApName.FIELD_ACCESS_POINT_ID));
        Predicate activeCond = join.get(ApName.FIELD_DELETE_CHANGE_ID).isNull();
        Predicate prefCond = cb.isTrue(join.get(ApName.FIELD_PREFERRED_NAME));
        join.on(cb.and(fkCond, activeCond, prefCond));
        return join;
    }
}
