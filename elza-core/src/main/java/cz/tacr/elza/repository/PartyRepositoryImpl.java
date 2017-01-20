package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.UsrPermissionView;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.enumeration.StringLength;
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
import javax.persistence.criteria.Subquery;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
                                                 final Set<Integer> registerTypeIds,
                                                 final Integer firstResult,
                                                 final Integer maxResults,
                                                 final Set<Integer> scopeIds,
                                                 final boolean readAllScopes,
                                                 final UsrUser user) {

        if(CollectionUtils.isEmpty(scopeIds)) {
            return Collections.EMPTY_LIST;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParParty> query = builder.createQuery(ParParty.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, partyTypeId, registerTypeIds, party, builder, scopeIds, readAllScopes, user, query);

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
                                            final Set<Integer> registerTypeIds,
                                            final Set<Integer> scopeIds,
                                            final boolean readAllScopes,
                                            final UsrUser user) {

        if(CollectionUtils.isEmpty(scopeIds)){
            return 0;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ParParty> party = query.from(ParParty.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, partyTypeId, registerTypeIds, party, builder, scopeIds, readAllScopes, user, query);

        query.select(builder.countDistinct(party));
        if (condition != null) {
            query.where(condition);
        }

        return entityManager.createQuery(query)
            .getSingleResult();
    }

    /**
     * Připraví dotaz pro nalezení rejstříkových záznamů.
     *  @param searchRecord      hledaný řetězec, může být null
     * @param partyTypeId       typ záznamu
     * @param builder           buider pro vytváření podmínek
     * @param scopeIds seznam tříd rejstříků, ve kterých se vyhledává
     * @param readAllScopes
     * @param user @return výsledné podmínky pro dotaz, nebo null pokud není za co filtrovat
     * @param query
     */
    private <T> Predicate preparefindRegRecordByTextAndType(final String searchRecord,
                                                        final Integer partyTypeId,
                                                        final Set<Integer> registerTypeIds,
                                                        final Root<ParParty> party,
                                                        final CriteriaBuilder builder,
                                                        final Set<Integer> scopeIds,
                                                        final boolean readAllScopes,
                                                        final UsrUser user,
                                                        final CriteriaQuery<T> query) {

        final String searchString = (searchRecord != null ? searchRecord.toLowerCase() : null);

        Join<Object, Object> record = party.join(ParParty.RECORD, JoinType.LEFT);
        Join<Object, Object> variantRecord = record.join(RegRecord.VARIANT_RECORD_LIST, JoinType.LEFT);
        Join<Object, Object> scope = record.join(RegRecord.SCOPE, JoinType.INNER);

        Join<Object, Object> partyType = party.join(ParParty.PARTY_TYPE);

        String searchValue = "%" + searchString + "%";

        List<Predicate> condition = new ArrayList<>();
        if (StringUtils.isNotBlank(searchString)) {
            condition.add(builder.or(
                    builder.like(builder.lower(record.get(RegRecord.RECORD)), searchValue),
                    builder.like(builder.lower(builder.substring(record.get(RegRecord.CHARACTERISTICS), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(builder.substring(record.get(RegRecord.NOTE), 1, StringLength.LENGTH_1000)), searchValue),
                    builder.like(builder.lower(variantRecord.get(RegVariantRecord.RECORD)), searchValue)
                )
            );
        }

        if (partyTypeId != null) {
            condition.add(builder.equal(partyType.get(ParPartyType.PARTY_TYPE_ID), partyTypeId));
        }

        if (!readAllScopes && user != null) {

            Subquery<UsrPermissionView> subquery = query.subquery(UsrPermissionView.class);
            Root<UsrPermissionView> rootSubquery = subquery.from(UsrPermissionView.class);
            subquery.select(rootSubquery.get(UsrPermissionView.SCOPE));
            subquery.where(builder.equal(rootSubquery.get(UsrPermissionView.USER), user));

            condition.add(scope.get(RegScope.SCOPE_ID).in(subquery));
        } else {
            condition.add(scope.get(RegScope.SCOPE_ID).in(scopeIds));
        }

        if (registerTypeIds != null) {
            condition.add(record.get(RegRecord.REGISTER_TYPE).in(registerTypeIds));

        }

        return builder.and(condition.toArray(new Predicate[condition.size()]));
    }

    @Override
    @Transactional
    public void unsetAllPreferredName() {
        entityManager.createQuery("update par_party set " + ParParty.PARTY_PREFERRED_NAME + " = null").executeUpdate();
    }
}
