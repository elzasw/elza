package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;

public class PartyPreferredNameWrapper implements EntityWrapper, EntityMetrics {

    private static final Logger logger = LoggerFactory.getLogger(PartyPreferredNameWrapper.class);

    private final PartyInfo partyInfo;

    private final EntityIdHolder<ParPartyName> partyNameIdHolder;

    private ParParty entity;

    PartyPreferredNameWrapper(PartyInfo partyInfo, EntityIdHolder<ParPartyName> partyNameIdHolder) {
        this.partyInfo = Validate.notNull(partyInfo);
        this.partyNameIdHolder = Validate.notNull(partyNameIdHolder);
    }

    @Override
    public PersistType getPersistType() {
        PersistType pt = partyInfo.getPersistType();
        // preferred name is always updated
        return pt.equals(PersistType.NONE) ? PersistType.NONE : PersistType.UPDATE;
    }

    @Override
    public ParParty getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        entity = partyInfo.getEntityRef(session);
        entity.setPreferredName(partyNameIdHolder.getEntityRef(session));
        entity = partyInfo.getEntityRef(session);

        /*
        logger.debug("Set prefered name for party, id = {}, class = {}, initialized = {}, objectId = {}",
                    partyInfo.getEntityId(),
                    entity.getClass(),
                    HibernateUtils.isInitialized(entity),
                    System.identityHashCode(entity));
          */

        ParPartyName preferedPartyName = partyNameIdHolder.getEntityRef(session);

        /*logger.debug("Set prefered name - nameId = {}, class = {}, initialized = {}", partyNameIdHolder.getEntityId(),
                    preferedPartyName.getClass(),
                    HibernateUtils.isInitialized(preferedPartyName));
          */

        entity.setPreferredName(preferedPartyName);
    }

    @Override
    public void afterEntityPersist() {
        partyInfo.onEntityPersist(getMemoryScore());
    }

    @Override
    public void evictFrom(Session session) {
        /*
        logger.info("Evict party after set prefered name, id = {}, class = {}, initialized = {}, objectId = {}",
                    partyInfo.getEntityId(),
                    entity.getClass(),
                    HibernateUtils.isInitialized(entity),
                    System.identityHashCode(entity));
                    */

        session.evict(entity);
    }
}
