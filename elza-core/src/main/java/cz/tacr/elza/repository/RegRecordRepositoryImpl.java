package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
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
import java.util.List;

/**
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class RegRecordRepositoryImpl implements RegRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<RegRecord> findRegRecordByTextAndType(final String searchRecord, final Integer registerTypeId,
                                                      final Integer firstReult, final Integer maxResults) {

        final String searchString = (searchRecord != null ? searchRecord.toLowerCase() : null);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegRecord> query = builder.createQuery(RegRecord.class);

        Root<RegRecord> record = query.from(RegRecord.class);
        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST, JoinType.LEFT);
        Join<Object, Object> registerType = record.join(RegRecord.REGISTER_TYPE);

        Predicate conditon = null;
        if (searchString != null) {
            conditon =  builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchString),
                    builder.like(builder.lower(record.get(RegRecord.CHARACTERISTICS)), searchString),
                    builder.like(builder.lower(record.get(RegRecord.COMMENT)), searchString),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchString)
            );
        }

        if (registerTypeId != null) {
            builder.and(
                    conditon,
                    builder.equal(registerType.get(RegRegisterType.ID), registerTypeId)
            );
        }

        Order order = builder.asc(record.get(RegRecord.RECORD));
        query.select(record).where(conditon).orderBy(order);

        return entityManager.createQuery(query)
                .setFirstResult(firstReult)
                .setMaxResults(maxResults)
                .getResultList();
    }
}
