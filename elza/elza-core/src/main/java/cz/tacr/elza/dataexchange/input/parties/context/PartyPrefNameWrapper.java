package cz.tacr.elza.dataexchange.input.parties.context;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.RefUpdateWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;

public class PartyPrefNameWrapper implements RefUpdateWrapper {

    private final PartyInfo partyInfo;

    private final EntityIdHolder<ParPartyName> partyNameIdHolder;

    private ParParty entity;

    PartyPrefNameWrapper(PartyInfo partyInfo, EntityIdHolder<ParPartyName> partyNameIdHolder) {
        this.partyInfo = Validate.notNull(partyInfo);
        this.partyNameIdHolder = Validate.notNull(partyNameIdHolder);
    }

    @Override
    public boolean isIgnored() {
        return partyInfo.getSaveMethod().equals(SaveMethod.IGNORE);
    }

    @Override
    public boolean isLoaded(Session session) {
        entity = partyInfo.getEntityRef(session);
        return HibernateUtils.isInitialized(entity);
    }

    @Override
    public void merge(Session session) {
        // prepare preferred name reference
        ParPartyName prefName = partyNameIdHolder.getEntityRef(session);
        entity.setPreferredName(prefName);
        // merge entity
        session.merge(entity);
        // update party info
        partyInfo.onEntityPersist();
    }

    @Override
    public void executeUpdateQuery(Session session) {
        // get name reference
        ParPartyName prefName = partyNameIdHolder.getEntityRef(session);
        // create query
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaUpdate<ParParty> query = cb.createCriteriaUpdate(ParParty.class);
        Root<ParParty> root = query.from(ParParty.class);
        query.set(ParParty.FIELD_PARTY_PREFERRED_NAME, prefName);
        query.where(cb.equal(root.get(ParParty.FIELD_PARTY_ID), partyInfo.getEntityId()));
        // execute query
        int affected = session.createQuery(query).executeUpdate();
        Validate.isTrue(affected == 1);
        // update party info
        partyInfo.onEntityPersist();        
    }
}
