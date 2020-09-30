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
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import cz.tacr.elza.domain.ApIndex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.ItemString;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME_LOWER;

/**
 * Implementace respozitory pro aprecord.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class ApAccessPointRepositoryImpl implements ApAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StaticDataService staticDataService;

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
        apStateRoot.fetch(ApState.FIELD_ACCESS_POINT, JoinType.INNER);

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

        // add name join
        Path<String> accessPointName = null;
        Path<String> userName = null;
        if (searchTypeName == SearchType.FULLTEXT || searchTypeName == SearchType.RIGHT_SIDE_LIKE) {
            Join<ApAccessPoint, ApPart> nameJoin = accessPoint.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.LEFT);
            Predicate nameFkCond = cb.equal(accessPoint.get(ApAccessPoint.FIELD_PREFFERED_PART),
                    nameJoin.get(ApPart.PART_ID));
            Predicate activeNameCond = nameJoin.get(ApPart.DELETE_CHANGE_ID).isNull();
            nameJoin.on(cb.and(nameFkCond, activeNameCond));
            Join<ApIndex, ApPart> indexJoin = nameJoin.join(ApPart.INDICES, JoinType.INNER);
            indexJoin.on(cb.equal(indexJoin.get(ApIndex.INDEX_TYPE), DISPLAY_NAME_LOWER));
            accessPointName = indexJoin.get(ApIndex.VALUE);
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
}
