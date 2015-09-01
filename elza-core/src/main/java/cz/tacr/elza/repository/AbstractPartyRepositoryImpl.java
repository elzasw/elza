package cz.tacr.elza.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ParAbstractParty;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;

/**
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class AbstractPartyRepositoryImpl implements AbstractPartyRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ParAbstractParty> findAbstractPartyByTextAndType(final String searchRecord, final Integer registerTypeId,
                                                      final Integer firstResult, final Integer maxResults) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParAbstractParty> query = builder.createQuery(ParAbstractParty.class);
        Root<ParAbstractParty> record = query.from(ParAbstractParty.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeId, record, builder);

        Order order = builder.asc(record.get(ParAbstractParty.ABSTRACT_PARTY_ID));
        query.select(record).where(condition).orderBy(order);

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findAbstractPartyByTextAndTypeCount(final String searchRecord, final Integer registerTypeId) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ParAbstractParty> record = query.from(ParAbstractParty.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeId, record, builder);

        query.select(builder.count(record)).where(condition);

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param partyTypeId    typ záznamu
     * @param record            kořen dotazu pro danou entitu
     * @param builder           buider pro vytváření podmínek
     * @return                  výsledné podmínky pro dotaz
     */
    private Predicate preparefindRegRecordByTextAndType(final String searchRecord, final Integer partyTypeId,
                                                   final Root<ParAbstractParty> party, final CriteriaBuilder builder) {

        final String searchString = (searchRecord != null ? searchRecord.toLowerCase() : null);

        Join<Object, Object> record = party.join(ParAbstractParty.RECORD, JoinType.LEFT);
        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST);

        Join<Object, Object> partySubtype = party.join(ParAbstractParty.PARTY_SUBTYPE);
        Join<Object, Object> partyType = partySubtype.join(ParPartySubtype.PARTY_TYPE);

        Predicate conditon = null;
        if (searchString != null) {
            conditon =  builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchString),
                    builder.like(builder.lower(record.get(RegRecord.CHARACTERISTICS)), searchString),
                    builder.like(builder.lower(record.get(RegRecord.COMMENT)), searchString),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchString)
            );
        }

        if (partyTypeId != null) {
            builder.and(
                    conditon,
                    builder.equal(partyType.get(ParPartyType.PARTY_TYPE_ID), partyTypeId)
            );
        }

        return conditon;
    }
}
