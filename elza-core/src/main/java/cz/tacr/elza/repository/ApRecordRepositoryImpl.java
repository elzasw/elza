package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApScope;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ApVariantRecord;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Implementace respozitory pro aprecord.
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class ApRecordRepositoryImpl implements ApRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ApRecord> findApRecordByTextAndType(final String searchRecord,
                                                    final Collection<Integer> apTypeIds,
                                                    final Integer firstReult,
                                                    final Integer maxResults,
                                                    final ApRecord parentRecord,
                                                    final Set<Integer> scopeIdsForSearch,
                                                    final Boolean excludeInvalid) {
        if(CollectionUtils.isEmpty(scopeIdsForSearch)){
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ApRecord> query = builder.createQuery(ApRecord.class);
        Root<ApRecord> record = query.from(ApRecord.class);

        Predicate condition = preparefindApRecordByTextAndType(searchRecord, apTypeIds, record, builder,
                scopeIdsForSearch, query, parentRecord, excludeInvalid);

        query.select(record).distinct(true);
        if (condition != null) {
            Order order = builder.asc(record.get(ApRecord.RECORD));
            query.where(condition).orderBy(order);
        }


        return entityManager.createQuery(query)
                .setFirstResult(firstReult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findApRecordByTextAndTypeCount(final String searchRecord,
                                               final Collection<Integer> apTypeIds,
                                               final ApRecord parentRecord,
                                               final Set<Integer> scopeIdsForSearch,
                                               boolean excludeInvalid) {
        if (CollectionUtils.isEmpty(scopeIdsForSearch)) {
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ApRecord> record = query.from(ApRecord.class);

        Predicate condition = preparefindApRecordByTextAndType(searchRecord, apTypeIds, record, builder,
                scopeIdsForSearch, query, parentRecord, excludeInvalid);

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
     * @param parentRecord
     * @param excludeInvalid
     * @return                  výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private <T> Predicate preparefindApRecordByTextAndType(final String searchRecord,
                                                           final Collection<Integer> apTypeId,
                                                           final Root<ApRecord> record,
                                                           final CriteriaBuilder builder,
                                                           final Set<Integer> scopeIdsForSearch,
                                                           final CriteriaQuery<T> query,
                                                           final ApRecord parentRecord,
                                                           final Boolean excludeInvalid) {
        Validate.notEmpty(scopeIdsForSearch);

        Join<Object, Object> variantRecord = record.join(ApRecord.VARIANT_RECORD_LIST, JoinType.LEFT);
        Join<Object, Object> apType = record.join(ApRecord.AP_TYPE);
        Join<Object, Object> scope = record.join(ApRecord.SCOPE, JoinType.INNER);

        Predicate condition = null;
        List<Predicate> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(searchRecord)) {
            final String searchValue = "%" + searchRecord.toLowerCase() + "%";
            condition =  builder.or(
                    builder.like(builder.lower(record.get(ApRecord.RECORD)), searchValue),
                    builder.like(builder.lower(builder.substring(record.get(ApRecord.CHARACTERISTICS), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(variantRecord.get(ApVariantRecord.RECORD)), searchValue)
            );
        }

        if (CollectionUtils.isNotEmpty(apTypeId)) {
            Predicate typePred = apType.get(ApType.ID).in(apTypeId);
            condition = condition == null ? typePred : builder.and(condition, typePred);
        }

        conditions.add(scope.get(ApScope.SCOPE_ID).in(scopeIdsForSearch));

        if (condition != null) {
            conditions.add(condition);
        }

        if (excludeInvalid != null && excludeInvalid) {
            conditions.add(builder.equal(record.get(ApRecord.INVALID), false));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }
}
