package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.UsrPermissionView;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.enumeration.StringLength;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
                                                      final Set<Integer> scopeIdsForRecord, final boolean readAllScopes, final UsrUser user) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return Collections.EMPTY_LIST;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder,
                scopeIdsForRecord, readAllScopes, user, query);

        if (parentRecord != null) {
            if (condition == null) {
                condition = builder.equal(record.get(RegRecord.PARENT_RECORD), parentRecord);
            } else {
                condition = builder.and(condition, builder.equal(record.get(RegRecord.PARENT_RECORD), parentRecord));
            }
        }

        // TODO Lebeda - kompatibilita dotazu !!!! nelze použít distinct na Text
        // query.select(record).distinct(true);
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
                                                final Set<Integer> scopeIdsForRecord,
                                                final boolean readAllScopes,
                                                final UsrUser user) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeIds, record, builder,
                scopeIdsForRecord, readAllScopes, user, query);

        if (parentRecord != null) {
            if (condition == null) {
                condition = builder.equal(record.get(RegRecord.PARENT_RECORD), parentRecord);
            } else {
                condition = builder.and(condition, builder.equal(record.get(RegRecord.PARENT_RECORD), parentRecord));
            }
        }

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
     * @param readAllScopes
     *@param user
     * @param query @return                  výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private <T> Predicate preparefindRegRecordByTextAndType(final String searchRecord,
                                                        final Collection<Integer> registerTypeId,
                                                        final Root<RegRecord> record,
                                                        final CriteriaBuilder builder,
                                                        final Set<Integer> scopeIdsForRecord,
                                                            final boolean readAllScopes,
                                                            final UsrUser user,
                                                            final CriteriaQuery<T> query) {
        Assert.notEmpty(scopeIdsForRecord);

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

        if (!readAllScopes && user != null) {

            Subquery<UsrPermissionView> subquery = query.subquery(UsrPermissionView.class);
            Root<UsrPermissionView> rootSubquery = subquery.from(UsrPermissionView.class);
            subquery.select(rootSubquery.get(UsrPermissionView.SCOPE));
            subquery.where(builder.equal(rootSubquery.get(UsrPermissionView.USER), user));

            conditions.add(scope.get(RegScope.SCOPE_ID).in(subquery));
        } else {
            conditions.add(scope.get(RegScope.SCOPE_ID).in(scopeIdsForRecord));
        }

        if (condition != null) {
            conditions.add(condition);
        }

        return builder.and(conditions.toArray(new Predicate[conditions.size()]));
    }


    /*@Override
    public long findRootRecordsByTypeCount(final Collection<Integer> registerTypeIds,
                                           final Set<Integer> scopeIdsForRecord) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(null, registerTypeIds, record, builder, scopeIdsForRecord, readAllScopes, user, query);
        if (condition == null) {
            condition = builder.isNull(record.get(RegRecord.PARENT_RECORD));
        } else {
            condition = builder.and(condition, builder.isNull(record.get(RegRecord.PARENT_RECORD)));
        }

        query.select(builder.countDistinct(record));
        query.where(condition);

        return entityManager.createQuery(query).getSingleResult();
    }*/

    /*@Override
    public List<RegRecord> findRootRecords(final Collection<Integer> registerTypeIds, final Integer firstResult,
                                           final Integer maxResults, final Set<Integer> scopeIdsForRecord) {
        if(CollectionUtils.isEmpty(scopeIdsForRecord)){
            return Collections.EMPTY_LIST;
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);
        Root<RegRecord> record = query.from(RegRecord.class);

        Predicate condition = preparefindRegRecordByTextAndType(null, registerTypeIds, record, builder,
                scopeIdsForRecord, readAllScopes, user, query);
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
    }*/

    @Override
    public List<Integer> findRecordParents(final Integer recordId) {
        StringBuilder sb = new StringBuilder();
        sb.append("select");
        sb.append(" r2.record_id as r2record_id, r3.record_id as r3record_id, r4.record_id as r4record_id,");
        sb.append(" r5.record_id as r5record_id, r6.record_id as r6record_id, r7.record_id as r7record_id,");
        sb.append(" r8.record_id as r8record_id, r9.record_id as r9record_id, r10.record_id as r10record_id");
        sb.append(" from reg_record r1");
        sb.append(" left join reg_record r2 on r1.parent_record_id = r2.record_id");
        sb.append(" left join reg_record r3 on r2.parent_record_id = r3.record_id");
        sb.append(" left join reg_record r4 on r3.parent_record_id = r4.record_id");
        sb.append(" left join reg_record r5 on r4.parent_record_id = r5.record_id");
        sb.append(" left join reg_record r6 on r5.parent_record_id = r6.record_id");
        sb.append(" left join reg_record r7 on r6.parent_record_id = r7.record_id");
        sb.append(" left join reg_record r8 on r7.parent_record_id = r8.record_id");
        sb.append(" left join reg_record r9 on r8.parent_record_id = r9.record_id");
        sb.append(" left join reg_record r10 on r9.parent_record_id = r10.record_id");

        sb.append(" where (r1.record_id = :recordId)");

        List<Integer> result = new LinkedList<>();

        // Čtení všech parentů
        Query query = entityManager.createNativeQuery(sb.toString());

        Integer findRecordParentsId = recordId;
        while (findRecordParentsId != null) {
            query.setParameter("recordId", findRecordParentsId);
            List<Object> rows = query.getResultList();
            if (!rows.isEmpty()) {
                Object[] row = (Object[]) rows.get(0);
                for (Object objectId : row) {
                    if (objectId != null) {
                        result.add(((Number) objectId).intValue());
                    }
                }

                Object lastObjectId = row[row.length - 1];
                if (lastObjectId != null) {
                    findRecordParentsId = ((Number) lastObjectId).intValue();
                } else {
                    findRecordParentsId = null;
                }
            } else {
                findRecordParentsId = null;
            }
        }

        return result;
    }
}
