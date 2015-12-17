package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

/**
 * Implementace respozitory pro regrecord.
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class RegRecordRepositoryImpl implements RegRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RegRecord> findRegRecordByTextAndType(final String searchRecord, final Collection<Integer> registerTypeIds,
                                                      final Integer firstReult, final Integer maxResults) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder);

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
    public long findRegRecordByTextAndTypeCount(final String searchRecord, final Collection<Integer> registerTypeIds) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder);

        query.select(builder.countDistinct(record));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param registerTypeId    ty záznamu
     * @param record            kořen dotazu pro danou entitu
     * @param builder           buider pro vytváření podmínek
     * @return                  výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private Predicate preparefindRegRecordByTextAndType(final String searchRecord, final Collection<Integer> registerTypeId,
                                                   final Root<RegRecord> record, final CriteriaBuilder builder) {
        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST, JoinType.LEFT);
        Join<Object, Object> registerType = record.join(RegRecord.REGISTER_TYPE);

        Predicate conditon = null;
        if (StringUtils.isNotBlank(searchRecord)) {
            final String searchValue = "%" + searchRecord.toLowerCase() + "%";
            conditon =  builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchValue),
                    builder.like(builder.lower(record.get(RegRecord.CHARACTERISTICS)), searchValue),
                    builder.like(builder.lower(record.get(RegRecord.NOTE)), searchValue),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchValue)
            );
        }

        if (CollectionUtils.isNotEmpty(registerTypeId)) {
            Predicate typePred = registerType.get(RegRegisterType.ID).in(registerTypeId);
            conditon = conditon == null ? typePred : builder.and(conditon, typePred);
        }

        return conditon;
    }
}
