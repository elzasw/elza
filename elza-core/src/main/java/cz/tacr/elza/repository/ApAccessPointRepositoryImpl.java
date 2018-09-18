package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;

import cz.tacr.elza.domain.ApState;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApName;

/**
 * Implementace respozitory pro aprecord.
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class ApAccessPointRepositoryImpl implements ApAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ApAccessPoint> findApAccessPointByTextAndType(@Nullable String searchRecord,
                                                              @Nullable Collection<Integer> apTypeIds,
                                                              Integer firstResult,
                                                              Integer maxResults,
                                                              Set<Integer> scopeIdsForSearch) {
        if(CollectionUtils.isEmpty(scopeIdsForSearch)){
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ApAccessPoint> query = builder.createQuery(ApAccessPoint.class);
        Root<ApAccessPoint> record = query.from(ApAccessPoint.class);

        Predicate condition = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIdsForSearch, record, builder, query, false);

        query.select(record);
        if (condition != null) {
            query.where(condition);
        }

        List<ApAccessPoint> resultList = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();

        return resultList.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public long findApAccessPointByTextAndTypeCount(String searchRecord,
                                                    Collection<Integer> apTypeIds,
                                                    Set<Integer> scopeIds) {
        if (CollectionUtils.isEmpty(scopeIds)) {
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ApAccessPoint> record = query.from(ApAccessPoint.class);

        Predicate condition = prepareApSearchPredicate(searchRecord, apTypeIds, scopeIds, record, builder, query, true);

        query.select(builder.countDistinct(record));
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
     * @param fromAp query root or entity join of AP
     * @param cb JPA query builder
     ** @param query
     * @return AP predicate which can be used as where condition.
     */
    public static Predicate prepareApSearchPredicate(final String searchValue,
                                                     final Collection<Integer> apTypeIds,
                                                     final Collection<Integer> scopeIds,
                                                     final From<?, ApAccessPoint> fromAp,
                                                     final CriteriaBuilder cb,
                                                     final CriteriaQuery<?> query,
                                                     final boolean count) {
        // prepare conjunction list
        List<Predicate> conjunctions = new ArrayList<>();

        // search only active AP
        conjunctions.add(fromAp.get(ApAccessPoint.DELETE_CHANGE_ID).isNull());
        conjunctions.add(cb.or(fromAp.get(ApAccessPoint.STATE).isNull(), cb.notEqual(fromAp.get(ApAccessPoint.STATE), ApState.TEMP)));

        // add name join
        Join<ApAccessPoint, ApName> nameJoin = fromAp.join(ApAccessPoint.NAMES, JoinType.LEFT);
        Predicate nameFkCond = cb.equal(fromAp.get(ApAccessPoint.ACCESS_POINT_ID),
                nameJoin.get(ApName.ACCESS_POINT_ID));
        Predicate activeNameCond = nameJoin.get(ApName.DELETE_CHANGE_ID).isNull();
        nameJoin.on(cb.and(nameFkCond, activeNameCond));

        if (!count) {
            query.orderBy(cb.asc(nameJoin.get(ApName.NAME)));
        }

        // add text search
        String searchExp = StringUtils.trimToNull(searchValue);
        if (searchExp != null) {
            searchExp = '%' + searchExp.toLowerCase() + '%';

            // add description join
            // Join<ApAccessPoint, ApDescription> descJoin = apJoin.join(ApAccessPoint.DESCRIPTIONS, JoinType.LEFT);
            // Predicate descFkCond = cb.equal(apJoin.get(ApAccessPoint.ACCESS_POINT_ID), descJoin.get(ApDescription.ACCESS_POINT_ID));
            // Predicate activeDescCond = descJoin.get(ApDescription.DELETE_CHANGE_ID).isNull();
            // descJoin.on(cb.and(descFkCond, activeDescCond));

            // add like condition to where
            Predicate nameLikeCond = cb.like(cb.lower(nameJoin.get(ApName.NAME)), searchExp);
            conjunctions.add(nameLikeCond);
        }

        // add scope id condition
        Validate.isTrue(!scopeIds.isEmpty());
        conjunctions.add(fromAp.get(ApAccessPoint.SCOPE_ID).in(scopeIds));

        // add AP type condition
        if (CollectionUtils.isNotEmpty(apTypeIds)) {
            conjunctions.add(fromAp.get(ApAccessPoint.AP_TYPE_ID).in(apTypeIds));
        }

        return cb.and(conjunctions.toArray(new Predicate[0]));
    }

    /**
     * Prepares inner join for preferred AP name (preferred name is always expected).
     *
     * @param fromAp query root or entity join of AP
     * @param cb JPA criteria builder
     *
     * @return Created preferred AP name join.
     */
    public static Join<ApAccessPoint, ApName> preparePrefNameJoin(From<?, ApAccessPoint> fromAp, CriteriaBuilder cb) {
        Join<ApAccessPoint, ApName> join = fromAp.join(ApAccessPoint.NAMES, JoinType.INNER);
        Predicate fkCond = cb.equal(fromAp.get(ApAccessPoint.ACCESS_POINT_ID),
                                        join.get(ApName.ACCESS_POINT_ID));
        Predicate activeCond = join.get(ApName.DELETE_CHANGE_ID).isNull();
        Predicate prefCond = cb.isTrue(join.get(ApName.PREFERRED_NAME));
        join.on(cb.and(fkCond, activeCond, prefCond));
        return join;
    }
}
