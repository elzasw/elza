package cz.tacr.elza.repository;

import java.util.ArrayList;
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
import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;

/**
 * Implementace repository osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class PartyRepositoryImpl implements PartyRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ParParty> findPartyByTextAndType(final String searchRecord, final Integer registerTypeId,
                                         final Integer firstResult, final Integer maxResults, final Boolean onlyLocal) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParParty> query = builder.createQuery(ParParty.class);
        Root<ParParty> record = query.from(ParParty.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeId, record, builder,
                onlyLocal);

        query.select(record).distinct(true);
        if (condition != null) {
            Order order = builder.asc(record.get(ParParty.ABSTRACT_PARTY_ID));
            query.where(condition).orderBy(order);
        }

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findPartyByTextAndTypeCount(final String searchRecord, final Integer registerTypeId, final Boolean onlyLocal) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ParParty> record = query.from(ParParty.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeId, record, builder,
                onlyLocal);

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
     * @param partyTypeId       typ záznamu
     * @param builder           buider pro vytváření podmínek
     * @param onlyLocal vyhledat pouze lokální nebo globální osoby
     * @return výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private Predicate preparefindRegRecordByTextAndType(final String searchRecord, final Integer partyTypeId,
                        final Root<ParParty> party, final CriteriaBuilder builder, final Boolean onlyLocal) {

        final String searchString = (searchRecord != null ? searchRecord.toLowerCase() : null);

        Join<Object, Object> record = party.join(ParParty.RECORD, JoinType.LEFT);
        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST, JoinType.LEFT);

        Join<Object, Object> partyType = party.join(ParParty.PARTY_TYPE);

        String searchValue = "%" + searchString + "%";

        List<Predicate> condition = new ArrayList<>();
        if (StringUtils.isNotBlank(searchString)) {
            condition.add(builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchValue),
                    builder.like(builder.lower(record.get(RegRecord.CHARACTERISTICS)), searchValue),
                    builder.like(builder.lower(record.get(RegRecord.NOTE)), searchValue),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchValue)
                )
            );
        }

        if (onlyLocal != null) {
            condition.add(builder.equal(record.get(RegRecord.LOCAL), onlyLocal));
        }

        if (partyTypeId != null) {
            condition.add(builder.equal(partyType.get(ParPartyType.PARTY_TYPE_ID), partyTypeId));
        }

        return builder.and(condition.toArray(new Predicate[condition.size()]));
    }

    @Override
    @Transactional
    public void unsetAllPreferredName() {
        entityManager.createQuery("update par_party set " + ParParty.PARTY_PREFERRED_NAME + " = null").executeUpdate();
    }
}
