package cz.tacr.elza.repository;

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
import javax.persistence.criteria.Subquery;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParParty;

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
                                                 final Set<Integer> scopeIds) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParParty> query = builder.createQuery(ParParty.class);
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ParParty> partySubquery = subquery.from(ParParty.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparePartyApSearchPredicate(searchRecord, partyTypeId, apTypeIds, scopeIds, party, builder, query, false, true);
        Predicate conditionSubquery = preparePartyApSearchPredicate(searchRecord, partyTypeId, apTypeIds, scopeIds, partySubquery, builder, null, true, false);

        subquery.where(conditionSubquery);
        subquery.select(partySubquery.get(ParParty.FIELD_PARTY_ID));

        query.select(party);
        if (condition != null) {
            Order order = builder.asc(party.get(ParParty.FIELD_PARTY_ID));
            query.where(condition, builder.in(party.get(ParParty.FIELD_PARTY_ID)).value(subquery)).orderBy(order);
        }

        Join<Object, Object> partyName = party.join(ParParty.FIELD_PARTY_PREFERRED_NAME, JoinType.LEFT);
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
                                            final Set<Integer> scopeIds) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparePartyApSearchPredicate(searchRecord, partyTypeId, apTypeIds, scopeIds, party, builder, query, true, false);

        query.select(builder.countDistinct(party));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    @Override
    @Transactional
    public void unsetAllPreferredName() {
        entityManager.createQuery("update par_party set " + ParParty.FIELD_PARTY_PREFERRED_NAME + " = null").executeUpdate();
    }
    
    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     *
     * @param searchValue
     *            hledaný řetězec, může být null
     * @param partyTypeId
     *            typ záznamu
     * @param scopeIds
     *            seznam tříd rejstříků, ve kterých se vyhledává
     * @param cb
     *            buider pro vytváření podmínek
     * @param query
     * @return výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private static Predicate preparePartyApSearchPredicate(final String searchValue,
                                                           final Integer partyTypeId,
                                                           final Set<Integer> apTypeIds,
                                                           final Set<Integer> scopeIds,
                                                           final Root<ParParty> partyRoot,
                                                           final CriteriaBuilder cb,
                                                           final CriteriaQuery<?> query,
                                                           final boolean count,
                                                           final boolean onlyPrefferedName) {
        // join AP which must always exists
        Join<ParParty, ApAccessPoint> apJoin = partyRoot.join(ParParty.FIELD_RECORD, JoinType.INNER);

        Predicate cond = ApAccessPointRepositoryImpl.prepareApSearchPredicate(searchValue, apTypeIds, scopeIds, apJoin, cb, query, count, onlyPrefferedName);
        // add party type condition
        if (partyTypeId != null) {
            Predicate typeCond = cb.equal(partyRoot.get(ParParty.FIELD_PARTY_TYPE_ID), partyTypeId);
            cond = cb.and(cond, typeCond);
        }

        return cond;
    }
}
