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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApName;
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
                                                 final Set<Integer> scopeIds,
                                                 final Boolean excludeInvalid) {

        if (CollectionUtils.isEmpty(scopeIds)) {
            return Collections.emptyList();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParParty> query = builder.createQuery(ParParty.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparefindApByTextAndType(searchRecord, partyTypeId, apTypeIds, scopeIds, excludeInvalid,
                                                         party, builder);

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

        if (CollectionUtils.isEmpty(scopeIds)) {
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparefindApByTextAndType(searchRecord, partyTypeId, apTypeIds, scopeIds, excludeInvalid,
                                                         party, builder);

        query.select(builder.countDistinct(party));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     * 
     * @param searchValue
     *            hledaný řetězec, může být null
     * @param partyTypeId
     *            typ záznamu
     * @param cb
     *            buider pro vytváření podmínek
     * @param scopeIds
     *            seznam tříd rejstříků, ve kterých se vyhledává
     * @param query
     * @param excludeInvalid
     * @return výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     */
    private Predicate preparefindApByTextAndType(final String searchValue,
                                                 final Integer partyTypeId,
                                                 final Set<Integer> apTypeIds,
                                                 final Set<Integer> scopeIds,
                                                 final Boolean excludeInvalid,
                                                 final Root<ParParty> root,
                                                 final CriteriaBuilder cb) {
        // join AP which must always exists
        Join<ParParty, ApAccessPoint> apJoin = root.join(ParParty.RECORD, JoinType.INNER);

        // prepare where conditions
        List<Predicate> conditions = new ArrayList<>();

        // add text search
        String searchExp = StringUtils.trimToNull(searchValue);
        if (searchExp != null) {
            searchExp = '%' + searchExp.toLowerCase() + '%';
            // add name join
            Join<ApAccessPoint, ApName> nameJoin = apJoin.join(ApAccessPoint.NAMES, JoinType.LEFT);
            Predicate nameFkCond = cb.equal(apJoin.get(ApAccessPoint.ACCESS_POINT_ID),
                                            nameJoin.get(ApName.ACCESS_POINT_ID));
            nameJoin.on(cb.and(nameFkCond, nameJoin.get(ApName.DELETE_CHANGE_ID).isNull()));
            // add description join
            // Join<ApAccessPoint, ApDescription> descJoin = apJoin.join(ApAccessPoint.DESCRIPTIONS, JoinType.LEFT);
            // Predicate descFkCond = cb.equal(apJoin.get(ApAccessPoint.ACCESS_POINT_ID), descJoin.get(ApDescription.ACCESS_POINT_ID));
            // descJoin.on(cb.and(descFkCond, descJoin.get(ApDescription.DELETE_CHANGE_ID).isNull()));
            // add like condition to where
            Predicate likeCond = cb.like(cb.lower(nameJoin.get(ApName.NAME)), searchExp);
            conditions.add(likeCond);
        }

        // add scope id condition
        Validate.isTrue(!scopeIds.isEmpty());
        conditions.add(apJoin.get(ApAccessPoint.SCOPE_ID).in(scopeIds));

        // add party type condition
        if (partyTypeId != null) {
            conditions.add(cb.equal(root.get(ParParty.PARTY_TYPE_ID), partyTypeId));
        }

        // add not invalid AP condition
        if (Boolean.TRUE.equals(excludeInvalid)) {
            conditions.add(cb.notEqual(apJoin.get(ApAccessPoint.INVALID), Boolean.TRUE));
        }

        // add AP type condition
        if (CollectionUtils.isNotEmpty(apTypeIds)) {
            conditions.add(apJoin.get(ApAccessPoint.AP_TYPE_ID).in(apTypeIds));
        }
        return cb.and(conditions.toArray(new Predicate[0]));
    }

    @Override
    @Transactional
    public void unsetAllPreferredName() {
        entityManager.createQuery("update par_party set " + ParParty.PARTY_PREFERRED_NAME + " = null").executeUpdate();
    }
}
