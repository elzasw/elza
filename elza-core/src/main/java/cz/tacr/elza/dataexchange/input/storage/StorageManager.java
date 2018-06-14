package cz.tacr.elza.dataexchange.input.storage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.dataexchange.input.aps.context.APVariantNameWrapper;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionWrapper;
import cz.tacr.elza.dataexchange.input.parties.aps.PartyAccessPointWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyGroupIdentifierWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyNameComplementWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyNameWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyPreferredNameWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyUnitDateWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrDataWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrDescItemWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrLevelWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrNodeRegisterWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrNodeWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrStructItemWrapper;
import cz.tacr.elza.dataexchange.input.sections.context.ArrStructObjectWrapper;

/**
 * Storage manager for all imported items. Must be initialized with active session.
 */
public class StorageManager implements StorageListener {

    static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final List<EntityWrapper> persistEntities = new LinkedList<>();

    private final long memoryScoreLimit;

    private final Session session;

    private final RegRecordStorage recordStorage;

    private final ParPartyStorage partyStorage;

    private long currentMemoryScore;

    public StorageManager(long memoryScoreLimit, Session session, ImportInitHelper initHelper) {
        this.memoryScoreLimit = memoryScoreLimit;
        this.session = session;
        this.recordStorage = new RegRecordStorage(this, LocalDateTime.now(), session, initHelper);
        this.partyStorage = new ParPartyStorage(this, session, initHelper);
    }

    /**
     * Clear all stored entities from persistent context. Unflushed changes to the entity will not
     * be synchronized with the database.
     */
    public void clear() {
        logger.debug("Clearing entities from persistent context, count: {}", persistEntities.size());

        for (EntityWrapper ew : persistEntities) {

            //logger.debug("Evicting wrapper, class = {}", ew.getClass());

            ew.evictFrom(session);
        }
        persistEntities.clear();
        currentMemoryScore = 0;
    }

    @Override
    public void onEntityPersist(EntityWrapper item) {
        Validate.notNull(item);

        persistEntities.add(item);
        // estimate memory score
        long memoryScore = 1;
        if (item instanceof EntityMetrics) {
            memoryScore = ((EntityMetrics) item).getMemoryScore();
        }
        // flush & clear when overflow the limit
        currentMemoryScore += memoryScore;
        if (currentMemoryScore > memoryScoreLimit) {
            session.flush();
            clear();
        }
    }

    public void saveAccessPoints(Collection<AccessPointWrapper> items) {
        recordStorage.save(items);
        session.flush();
    }

    public void saveAPVariantNames(Collection<APVariantNameWrapper> items) {
        saveEntities(items);
    }

    public void saveParties(Collection<PartyWrapper> items) {
        partyStorage.save(items);
        session.flush();
    }

    public void savePartyUnitDates(Collection<PartyUnitDateWrapper> items) {
        saveEntities(items);
    }

    public void savePartyGroupIdentifiers(Collection<PartyGroupIdentifierWrapper> items) {
        saveEntities(items);
    }

    public void savePartyNames(Collection<PartyNameWrapper> items) {
        saveEntities(items);
    }

    public void savePartyNameComplements(Collection<PartyNameComplementWrapper> items) {
        saveEntities(items);
    }

    public void savePartyPreferredNames(Collection<PartyPreferredNameWrapper> items) {
        saveEntities(items);
    }

    public void saveInstitutions(Collection<InstitutionWrapper> items) {
        saveEntities(items);
    }

    public void savePartyAccessPoints(Collection<PartyAccessPointWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionNodes(Collection<ArrNodeWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionNodeRegistry(Collection<ArrNodeRegisterWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionLevels(Collection<ArrLevelWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionDescItems(Collection<ArrDescItemWrapper> items) {
        saveEntities(items);
    }

    public void saveStructItems(Collection<ArrStructItemWrapper> items) {
        saveEntities(items);
    }

    public void saveStructObjects(Collection<ArrStructObjectWrapper> items) {
        saveEntities(items);
    }

    public void saveData(Collection<ArrDataWrapper> items) {
        saveEntities(items);
    }

    /**
     * Stores all items and flushes persistent context.
     */
    private <T extends EntityWrapper> void saveEntities(Collection<T> items) {
        if (items.isEmpty()) {
            return;
        }
        EntityStorage<T> storage = new EntityStorage<>(session, this);
        storage.save(items);
        session.flush();
    }
}
