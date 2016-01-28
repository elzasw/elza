package cz.tacr.elza.repository;

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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;

/**
 * Implementace respozitory pro regrecord.
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class RegRecordRepositoryImpl implements RegRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RegRecord> findRegRecordByTextAndType(final String searchRecord,
                                                      final Collection<Integer> registerTypeIds,
                                                      final Integer firstReult,
                                                      final Integer maxResults,
                                                      final RegRecord parentRecord,
                                                      final Set<Integer> scopeIdsForRecord) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return Collections.EMPTY_LIST;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder,
                scopeIdsForRecord);

        query.select(record).distinct(true);
        if (condition != null) {
            Order order = builder.asc(record.get(RegRecord.RECORD));
            query.where(condition).orderBy(order);
        }


        return entityManager.createQuery(query)
                .setFirstResult(firstReult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findRegRecordByTextAndTypeCount(final String searchRecord,
                                                final Collection<Integer> registerTypeIds,
                                                final RegRecord parentRecord,
                                                final Set<Integer> scopeIdsForRecord) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder,
                scopeIdsForRecord);

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
     * @param registerTypeId    ty záznamu
     * @param record            kořen dotazu pro danou entitu
     * @param builder           buider pro vytváření podmínek
     * @param scopeIdsForRecord id tříd, do který spadají rejstříky
     * @return                  výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private Predicate preparefindRegRecordByTextAndType(final String searchRecord,
                                                        final Collection<Integer> registerTypeId,
                                                        final Root<RegRecord> record,
                                                        final CriteriaBuilder builder,
                                                        final Set<Integer> scopeIdsForRecord) {
        Assert.notEmpty(scopeIdsForRecord);

        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST, JoinType.LEFT);
        Join<Object, Object> registerType = record.join(RegRecord.REGISTER_TYPE);
        Join<Object, Object> scope = record.join(RegRecord.SCOPE, JoinType.INNER);

        Predicate condition = null;
        if (StringUtils.isNotBlank(searchRecord)) {
            final String searchValue = "%" + searchRecord.toLowerCase() + "%";
            condition =  builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchValue),
                    builder.like(builder.lower(record.get(RegRecord.CHARACTERISTICS)), searchValue),
                    builder.like(builder.lower(record.get(RegRecord.NOTE)), searchValue),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchValue)
            );
        }

        if (CollectionUtils.isNotEmpty(registerTypeId)) {
            Predicate typePred = registerType.get(RegRegisterType.ID).in(registerTypeId);
            condition = condition == null ? typePred : builder.and(condition, typePred);
        }


        Predicate scopeCondition = scope.get(RegScope.SCOPE_ID).in(scopeIdsForRecord);
        condition = condition == null ? scopeCondition : builder.and(condition, scopeCondition);

        return condition;
    }


    @Override
    public long findRootRecordsByTypeCount(final Collection<Integer> registerTypeIds,
                                           final Set<Integer> scopeIdsForRecord) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(null, registerTypeIds, record, builder, scopeIdsForRecord);
        if (condition == null) {
            condition = builder.isNull(record.get(RegRecord.PARENT_RECORD));
        } else {
            condition = builder.and(condition, builder.isNull(record.get(RegRecord.PARENT_RECORD)));
        }

        query.select(builder.countDistinct(record));
        query.where(condition);

        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public List<RegRecord> findRootRecords(Collection<Integer> registerTypeIds, Integer firstResult,
                                           Integer maxResults, final Set<Integer> scopeIdsForRecord) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return Collections.EMPTY_LIST;
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(null, registerTypeIds, record, builder,
                scopeIdsForRecord);
        if (condition == null) {
            condition = builder.isNull(record.get(RegRecord.PARENT_RECORD));
        } else {
            condition = builder.and(condition, builder.isNull(record.get(RegRecord.PARENT_RECORD)));
        }

        query.select(record).distinct(true);
        Order order = builder.asc(record.get(RegRecord.RECORD));
        query.where(condition).orderBy(order);

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }
}
