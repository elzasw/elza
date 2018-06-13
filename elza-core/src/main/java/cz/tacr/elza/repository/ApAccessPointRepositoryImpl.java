package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.enumeration.StringLength;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
                                                              Set<Integer> scopeIdsForSearch,
                                                              Boolean excludeInvalid) {
        if(CollectionUtils.isEmpty(scopeIdsForSearch)){
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ApAccessPoint> query = builder.createQuery(ApAccessPoint.class);
        Root<ApAccessPoint> record = query.from(ApAccessPoint.class);

        Predicate condition = preparefindApAccessPointByTextAndType(searchRecord, apTypeIds, record, builder,
                scopeIdsForSearch, query, excludeInvalid);

        query.select(record).distinct(true);
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findApAccessPointByTextAndTypeCount(String searchRecord,
                                                    Collection<Integer> apTypeIds,
                                                    Set<Integer> scopeIds,
                                                    boolean excludeInvalid) {
        if (CollectionUtils.isEmpty(scopeIds)) {
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ApAccessPoint> record = query.from(ApAccessPoint.class);

        Predicate condition = preparefindApAccessPointByTextAndType(searchRecord, apTypeIds, record, builder,
                scopeIds, query, excludeInvalid);

        query.select(builder.countDistinct(record));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query).getSingleResult();
    }


    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param apTypeId    ty záznamu
     * @param record            kořen dotazu pro danou entitu
     * @param builder           buider pro vytváření podmínek
     * @param scopeIdsForSearch id tříd, do který spadají rejstříky
     * @param query
     * @param excludeInvalid
     * @return                  výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private <T> Predicate preparefindApAccessPointByTextAndType(final String searchRecord,
                                                                final Collection<Integer> apTypeId,
                                                                final Root<ApAccessPoint> record,
                                                                final CriteriaBuilder builder,
                                                                final Set<Integer> scopeIdsForSearch,
                                                                final CriteriaQuery<T> query,
                                                                final Boolean excludeInvalid) {
        throw new UnsupportedOperationException();
        
        /* Validate.notEmpty(scopeIdsForSearch);

        Join<Object, Object> variantRecord = record.join(ApAccessPoint.NAME_LIST, JoinType.LEFT);
        Join<Object, Object> apType = record.join(ApAccessPoint.AP_TYPE);
        Join<Object, Object> scope = record.join(ApAccessPoint.SCOPE, JoinType.INNER);
        Join<ApAccessPoint, ApDescription> description = record.join(ApAccessPoint.DESCRIPTION_LIST);

        Predicate condition = null;
        List<Predicate> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(searchRecord)) {
            final String searchValue = "%" + searchRecord.toLowerCase() + "%";
            condition =  builder.or(
                    builder.like(builder.lower(builder.substring(description.get(ApDescription.DESCRIPTION), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(variantRecord.get(ApName.NAME)), searchValue)
            );
        }

        if (CollectionUtils.isNotEmpty(apTypeId)) {
            Predicate typePred = apType.get(ApType.ID).in(apTypeId);
            condition = condition == null ? typePred : builder.and(condition, typePred);
        }

        conditions.add(scope.get(ApScope.SCOPE_ID).in(scopeIdsForSearch));
        conditions.add(description.get(ApDescription.DELETE_CHANGE).isNull());

        if (condition != null) {
            conditions.add(condition);
        }

        if (excludeInvalid != null && excludeInvalid) {
            conditions.add(builder.equal(record.get(ApAccessPoint.INVALID), false));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()])); */
    }
}
