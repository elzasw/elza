package cz.tacr.elza.repository;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME_LOWER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.ItemString;

/**
 * Implementace respozitory pro aprecord.
 *
 */
@Component
public class ApAccessPointRepositoryImpl implements ApAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StaticDataService staticDataService;

    public static final String STAR = "*";

    @Override
    public List<ApState> findApAccessPointByTextAndType(
                                                        @Nullable String searchRecord,
                                                        @Nullable Collection<Integer> apTypeIds,
                                                        Integer firstResult,
                                                        Integer maxResults,
                                                        OrderBy orderBy,
                                                        Set<Integer> scopeIds,
                                                        @Nullable Collection<ApState.StateApproval> approvalStates,
                                                        @Nullable SearchType searchTypeName,
                                                        @Nullable SearchType searchTypeUsername) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ApState> query = builder.createQuery(ApState.class);
        //Root<ApAccessPoint> accessPointRoot = query.from(ApAccessPoint.class);        
        Root<ApState> apStateRoot = query.from(ApState.class);
        Fetch<ApState, ApAccessPoint> apFetch = apStateRoot.fetch(ApState.FIELD_ACCESS_POINT, JoinType.INNER);
        Join<ApState, ApAccessPoint> apJoin = (Join<ApState, ApAccessPoint>) apFetch;

        // query AP IDs
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ApState> apStateSubquery = subquery.from(ApState.class);
        Predicate conditionSubquery = prepareSearchPredicateUnordered(searchRecord, apTypeIds, scopeIds, approvalStates,
                                                                      apStateSubquery, builder,
                                                                      searchTypeName, searchTypeUsername);
        subquery.select(apStateSubquery.get(ApState.FIELD_ACCESS_POINT_ID));
        subquery.where(conditionSubquery);

        query.select(apStateRoot);

        ArrayList<Predicate> whereConds = new ArrayList<>();
        // search only active AP
        whereConds.add(apStateRoot.get(ApState.FIELD_DELETE_CHANGE_ID).isNull());
        whereConds.add(builder.in(apStateRoot.get(ApState.FIELD_ACCESS_POINT_ID)).value(subquery));

        if (orderBy == OrderBy.LAST_CHANGE) {
            // ordered by lastUpdate
            query.orderBy(builder.asc(apJoin.get(ApAccessPoint.FIELD_LAST_UPDATE)));
        } else {
            Root<ApIndex> indexRoot = query.from(ApIndex.class);
            whereConds.add(builder.equal(indexRoot.get(ApIndex.PART_ID),
                                         apJoin.get(ApAccessPoint.FIELD_PREFFERED_PART_ID)));
            whereConds.add(builder.equal(indexRoot.get(ApIndex.INDEX_TYPE), DISPLAY_NAME_LOWER));
            query.orderBy(builder.asc(indexRoot.get(ApIndex.VALUE)),
                          builder.asc(apJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID)));
        }
        query.where(whereConds.toArray(new Predicate[whereConds.size()]));

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
        // Root<ApAccessPoint> accessPoint = query.from(ApAccessPoint.class);
        Root<ApState> apState = query.from(ApState.class);

        Predicate condition = prepareSearchPredicateUnordered(searchRecord, apTypeIds, scopeIds,
                                                              approvalStates, apState,
                                                       builder,
                                                       searchTypeName, searchTypeUsername);

        // Distinct could be neeeded in some cases
        // we should switch to: builder.countDistinct...
        Expression<Long> count = builder.count(apState.get(ApState.FIELD_ACCESS_POINT_ID));

        query.select(count);
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Prepares predicate for AP search. Only necessary entities will be joined.
     * Prediacate cannot be used for ordering
     *
     * @param searchValue
     *            if present APs with partial match in name or description are
     *            returned
     * @param apTypeIds
     *            if not empty APs with same type are returned
     * @param scopeIds
     *            APs with same scope are returned, cannot be empty
     * @param accessPoint
     *            query root or entity join of AP
     * @param cb
     *            JPA query builder
     * @return AP predicate which can be used as where condition.
     */
    public static Predicate prepareSearchPredicateUnordered(@Nullable String searchValue,
                                                            @Nullable final Collection<Integer> apTypeIds,
                                                            final Collection<Integer> scopeIds,
                                                            @Nullable final Collection<ApState.StateApproval> approvalStates,
                                                            final From<?, ApState> apState,
                                                            final CriteriaBuilder cb,
                                                            @Nullable SearchType searchTypeName,
                                                            @Nullable SearchType searchTypeUsername) {
        searchValue = StringUtils.trimToNull(searchValue);
        Join<ApState, ApAccessPoint> apJoin = null;

        if (searchValue == null) {
            // Disable on empty search value
            searchTypeName = SearchType.DISABLED;
            searchTypeUsername = SearchType.DISABLED;
        } else {
            searchTypeName = searchTypeName != null ? searchTypeName : SearchType.FULLTEXT;
            searchTypeUsername = searchTypeUsername != null ? searchTypeUsername : SearchType.DISABLED;

            if (searchTypeName != SearchType.DISABLED || searchTypeUsername != SearchType.DISABLED) {
                // join AccessPoint
                apJoin = apState.join(ApState.FIELD_ACCESS_POINT);
            }
        }

        // prepare conjunction list
        List<Predicate> conjunctions = new ArrayList<>();

        // search only active AP
        conjunctions.add(apState.get(ApState.FIELD_DELETE_CHANGE_ID).isNull());

        // add text search
        Predicate nameLikeCond = null;
        Predicate usernameLikeCond = null;

        // add name join
        Path<String> accessPointName = null;
        Path<String> userName = null;
        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            Join<ApAccessPoint, ApPart> nameJoin = apJoin.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.LEFT);
            Predicate nameFkCond = cb.equal(apJoin.get(ApAccessPoint.FIELD_PREFFERED_PART),
                                            nameJoin.get(ApPart.PART_ID));
            Predicate activeNameCond = nameJoin.get(ApPart.DELETE_CHANGE_ID).isNull();
            nameJoin.on(cb.and(nameFkCond, activeNameCond));
            Join<ApIndex, ApPart> indexJoin = nameJoin.join(ApPart.INDICES, JoinType.INNER);
            indexJoin.on(cb.equal(indexJoin.get(ApIndex.INDEX_TYPE), DISPLAY_NAME_LOWER));
            accessPointName = indexJoin.get(ApIndex.VALUE);

            String searchNameExp = searchValue;
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

                // add like condition to where
                nameLikeCond = cb.like(cb.lower(accessPointName), searchNameExp);
            }
        }

        if (searchTypeUsername == SearchType.JOIN) {
            Join<ApAccessPoint, UsrUser> userJoin = apJoin.join(ApAccessPoint.FIELD_USER_LIST, JoinType.INNER);
            Predicate userFkCond = cb.equal(apJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID),
                                            userJoin.get(UsrUser.FIELD_ACCESS_POINT));
            Predicate activeUserCond = cb.equal(userJoin.get(UsrUser.FIELD_ACTIVE), true);
            userJoin.on(cb.and(userFkCond, activeUserCond));

        } else if (searchTypeUsername == SearchType.FULLTEXT || searchTypeUsername == SearchType.RIGHT_SIDE_LIKE) {
            Join<ApAccessPoint, UsrUser> userJoin = apJoin.join(ApAccessPoint.FIELD_USER_LIST, JoinType.INNER);
            Predicate userFkCond = cb.equal(apJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID),
                                            userJoin.get(UsrUser.FIELD_ACCESS_POINT));
            Predicate activeUserCond = cb.equal(userJoin.get(UsrUser.FIELD_ACTIVE), true);
            userJoin.on(cb.or(userFkCond, activeUserCond));

            userName = userJoin.get(UsrUser.FIELD_USERNAME);

            String searchUsernameExp = searchValue;
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
                usernameLikeCond = cb.like(cb.lower(userName), searchUsernameExp);
            }
        }

        if (nameLikeCond != null && usernameLikeCond == null) {
            conjunctions.add(nameLikeCond);
        }
        if (nameLikeCond == null && usernameLikeCond != null) {
            conjunctions.add(usernameLikeCond);
        }
        if (nameLikeCond != null && usernameLikeCond != null) {
            conjunctions.add(cb.or(nameLikeCond, usernameLikeCond));
        }

        // add scope id condition
        Validate.isTrue(!scopeIds.isEmpty());
        conjunctions.add(apState.get(ApState.FIELD_SCOPE_ID).in(scopeIds));

        // add AP type condition
        if (CollectionUtils.isNotEmpty(apTypeIds)) {
            conjunctions.add(apState.get(ApState.FIELD_AP_TYPE_ID).in(apTypeIds));
        }

        // add states
        if (CollectionUtils.isNotEmpty(approvalStates)) {
            conjunctions.add(apState.get(ApState.FIELD_STATE_APPROVAL).in(approvalStates));
        }

        return cb.and(conjunctions.toArray(new Predicate[0]));
    }

    public List<ApAccessPoint> findAccessPointsBySinglePartValues(List<Object> criterias) {
        StaticDataProvider sdp = staticDataService.getData();

        StringBuilder sb = new StringBuilder();
        sb.append("select ap.* from ap_access_point ap" + 
                " join ap_state aps ON aps.access_point_id = ap.access_point_id" +
                " where aps.delete_change_id is null and ap.access_point_id in (");
        sb.append("select p.access_point_id from ap_part p");
        // add conditions
        int counter = 1;
        for (Object criteria : criterias) {
            addCriteriasForPart(sdp, sb, counter, criteria);
            counter++;
        }
        sb.append(" where p.delete_change_id is null");
        sb.append(")");
        Query q = this.entityManager.createNativeQuery(sb.toString(), ApAccessPoint.class);

        return q.getResultList();
    }

    private void addCriteriasForPart(StaticDataProvider sdp, StringBuilder sb, int index, Object criteria) {
        if (criteria instanceof ItemString) {
            ItemString item = (ItemString) criteria;
            ItemType itemType = sdp.getItemTypeByCode(item.getType());
            Validate.isTrue(itemType.getDataType() == DataType.STRING);
            RulItemSpec itemSpec = null;
            if (itemType.hasSpecifications()) {
                if (item.getSpec() != null) {
                    itemSpec = itemType.getItemSpecByCode(item.getSpec());
                }
            }

            addJoinItem(sb, index, itemType, itemSpec);

            sb.append(" join arr_data_string d").append(index)
                    .append(" on d").append(index).append(".data_id = i").append(index).append(".data_id")
                    .append(" and d").append(index).append(".value='").append(item.getValue()).append("'");

        } else if (criteria instanceof ItemEnum) {
            ItemEnum item = (ItemEnum) criteria;
            ItemType itemType = sdp.getItemTypeByCode(item.getType());
            Validate.isTrue(itemType.getDataType() == DataType.ENUM);
            RulItemSpec itemSpec = itemType.getItemSpecByCode(item.getSpec());

            addJoinItem(sb, index, itemType, itemSpec);
        } else {
            throw new IllegalStateException("Unrecognized object: " + criteria);
        }

    }

    private void addJoinItem(StringBuilder sb, int index, ItemType itemType, RulItemSpec itemSpec) {

        sb.append(" join ap_item i").append(index)
                .append(" on i").append(index).append(".part_id = p.part_id ")
                .append(" and i").append(index).append(".delete_change_id is null")
                .append(" and i").append(index).append(".item_type_id = ").append(itemType.getItemTypeId());

        if (itemSpec != null) {
            sb.append(" and i").append(index).append(".item_spec_id = ").append(itemSpec.getItemSpecId());
        }

    }

    @Override
    public ScrollableResults findUncachedAccessPoints() {
        String hql = "SELECT ap.accessPointId FROM ap_access_point ap LEFT JOIN ap_cached_access_point cap ON cap.accessPointId = ap.accessPointId WHERE cap IS NULL";

        Session session = entityManager.unwrap(Session.class);
        ScrollableResults scrollableResults = session.createQuery(hql).setCacheMode(CacheMode.IGNORE)
                .scroll(ScrollMode.FORWARD_ONLY);

        return scrollableResults;
    }


}
