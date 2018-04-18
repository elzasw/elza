package cz.tacr.elza.repository;

import java.util.ArrayList;
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
import javax.transaction.Transactional;

import cz.tacr.elza.domain.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.enumeration.StringLength;

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
    public List<ParParty> findPartyByTextAndType(final String searchRecord,
                                                 final Integer partyTypeId,
                                                 final Set<Integer> apTypeIds,
                                                 final Integer firstResult,
                                                 final Integer maxResults,
                                                 final Set<Integer> scopeIds,
                                                 final Boolean excludeInvalid) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParParty> query = builder.createQuery(ParParty.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparefindApRecordByTextAndType(searchRecord, partyTypeId, apTypeIds, party,
                builder, scopeIds, query, excludeInvalid);

        query.select(party);
        if (condition != null) {
            Order order = builder.asc(party.get(ParParty.ABSTRACT_PARTY_ID));
            query.where(condition).orderBy(order);
        }

        Join<Object, Object> partyName = party.join(ParParty.PARTY_PREFERRED_NAME, JoinType.LEFT);
        query.orderBy(builder.asc(partyName.get("mainPart")), builder.asc(partyName.get("otherPart")));


        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findPartyByTextAndTypeCount(final String searchRecord,
                                            final Integer partyTypeId,
                                            final Set<Integer> apTypeIds,
                                            final Set<Integer> scopeIds,
                                            final Boolean excludeInvalid) {

        if(CollectionUtils.isEmpty(scopeIds)){
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparefindApRecordByTextAndType(searchRecord, partyTypeId, apTypeIds, party,
                builder, scopeIds, query, excludeInvalid);

        query.select(builder.countDistinct(party));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query)
            .getSingleResult();
    }

    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     * @param searchRecord      hledaný řetězec, může být null
     * @param partyTypeId       typ záznamu
     * @param builder           buider pro vytváření podmínek
     * @param scopeIds seznam tříd rejstříků, ve kterých se vyhledává
     * @param query
     * @param excludeInvalid
     * @return výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private <T> Predicate preparefindApRecordByTextAndType(final String searchRecord,
                                                           final Integer partyTypeId,
                                                           final Set<Integer> apTypeIds,
                                                           final Root<ParParty> party,
                                                           final CriteriaBuilder builder,
                                                           final Set<Integer> scopeIds,
                                                           final CriteriaQuery<T> query,
                                                           final Boolean excludeInvalid) {

        final String searchString = (searchRecord != null ? searchRecord.toLowerCase() : null);

        Join<Object, Object> record = party.join(ParParty.RECORD, JoinType.LEFT);
        Join<ApAccessPoint, ApName> variantRecord = record.join(ApAccessPoint.NAME_LIST, JoinType.LEFT);
        Join<Object, Object> scope = record.join(ApAccessPoint.SCOPE, JoinType.INNER);
        Join<ApAccessPoint, ApDescription> descriptionJoin = record.join(ApAccessPoint.DESCRIPTION_LIST);
        Join<Object, Object> partyType = party.join(ParParty.PARTY_TYPE);

        String searchValue = "%" + searchString + "%";

        List<Predicate> condition = new ArrayList<>();
        if (StringUtils.isNotBlank(searchString)) {
            condition.add(builder.or(
                    builder.like(builder.lower(builder.substring(descriptionJoin.get(ApDescription.DESCRIPTION), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(variantRecord.get(ApName.NAME)), searchValue)
                )
            );
        }

        if (partyTypeId != null) {
            condition.add(builder.equal(partyType.get(ParPartyType.PARTY_TYPE_ID), partyTypeId));
        }

        if (excludeInvalid != null && excludeInvalid) {
            condition.add(builder.equal(record.get(ApAccessPoint.INVALID), false));
        }

        condition.add(scope.get(ApScope.SCOPE_ID).in(scopeIds));

        if (CollectionUtils.isNotEmpty(apTypeIds)) {
            condition.add(record.get(ApAccessPoint.AP_TYPE).in(apTypeIds));
        }

        return builder.and(condition.toArray(new Predicate[condition.size()]));
    }

    @Override
    @Transactional
    public void unsetAllPreferredName() {
        entityManager.createQuery("update par_party set " + ParParty.PARTY_PREFERRED_NAME + " = null").executeUpdate();
    }
}
