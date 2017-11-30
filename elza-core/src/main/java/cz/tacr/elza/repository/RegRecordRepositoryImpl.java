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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.DatabaseType;
import cz.tacr.elza.core.RecursiveQueryBuilder;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.enumeration.StringLength;

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
                                                      final Set<Integer> scopeIdsForSearch,
                                                      final Boolean excludeInvalid) {
        if(CollectionUtils.isEmpty(scopeIdsForSearch)){
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder,
                scopeIdsForSearch, query, parentRecord, excludeInvalid);

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
                                                final Set<Integer> scopeIdsForSearch,
                                                boolean excludeInvalid) {
        if (CollectionUtils.isEmpty(scopeIdsForSearch)) {
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder,
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
     * @param registerTypeId    ty záznamu
     * @param record            kořen dotazu pro danou entitu
     * @param builder           buider pro vytváření podmínek
     * @param scopeIdsForSearch id tříd, do který spadají rejstříky
     * @param query
     * @param parentRecord
     * @param excludeInvalid
     * @return                  výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private <T> Predicate preparefindRegRecordByTextAndType(final String searchRecord,
                                                            final Collection<Integer> registerTypeId,
                                                            final Root<RegRecord> record,
                                                            final CriteriaBuilder builder,
                                                            final Set<Integer> scopeIdsForSearch,
                                                            final CriteriaQuery<T> query,
                                                            final RegRecord parentRecord,
                                                            final Boolean excludeInvalid) {
        Validate.notEmpty(scopeIdsForSearch);

        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST, JoinType.LEFT);
        Join<Object, Object> registerType = record.join(RegRecord.REGISTER_TYPE);
        Join<Object, Object> scope = record.join(RegRecord.SCOPE, JoinType.INNER);

        Predicate condition = null;
        List<Predicate> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(searchRecord)) {
            final String searchValue = "%" + searchRecord.toLowerCase() + "%";
            condition =  builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchValue),
                    builder.like(builder.lower(builder.substring(record.get(RegRecord.CHARACTERISTICS), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(builder.substring(record.get(RegRecord.NOTE), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchValue)
            );
        }

        if (CollectionUtils.isNotEmpty(registerTypeId)) {
            Predicate typePred = registerType.get(RegRegisterType.ID).in(registerTypeId);
            condition = condition == null ? typePred : builder.and(condition, typePred);
        }

        conditions.add(scope.get(RegScope.SCOPE_ID).in(scopeIdsForSearch));

        if (condition != null) {
            conditions.add(condition);
        }

        if (parentRecord != null) {
            conditions.add(builder.equal(record.get(RegRecord.PARENT_RECORD), parentRecord));
        }

        if (excludeInvalid != null && excludeInvalid) {
            conditions.add(builder.equal(record.get(RegRecord.INVALID), false));
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }

    @Override
    public List<RegRecord> findAccessPointsWithParents(Collection<Integer> apIds) {

        RecursiveQueryBuilder<RegRecord> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(RegRecord.class);

        rqBuilder.addSqlPart("WITH RECURSIVE apTree(record_id, register_type_id, record, characteristics, note, external_id, ")
                .addSqlPart("version, parent_record_id, scope_id, uuid, last_update, external_system_id, invalid, source_id, path) AS ")

                .addSqlPart("(SELECT r.*, r.record_id, 0 FROM reg_record r WHERE record_id in (:apIds) ")
                .addSqlPart("UNION ALL ")
                .addSqlPart("SELECT r.*, apt.source_id, apt.path + 1 FROM reg_record r ")
                .addSqlPart("JOIN apTree apt ON apt.parent_record_id=r.record_id)")

                .addSqlPart("SELECT * FROM apTree apt ORDER BY apt.source_id, apt.path desc");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("apIds", apIds);

        org.hibernate.query.Query<RegRecord> query = rqBuilder.getQuery();
        query.setCacheMode(CacheMode.IGNORE);
        return query.getResultList();
    }
}
