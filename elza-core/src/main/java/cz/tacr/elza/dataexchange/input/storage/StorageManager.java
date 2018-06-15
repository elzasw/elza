package cz.tacr.elza.dataexchange.input.storage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyWrapper;

/**
 * Storage manager for all imported items. Must be initialized with active
 * session.
 */
    static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final List<EntityWrapper> persistEntities = new LinkedList<>();

    private final long memoryScoreLimit;

    private final Session session;

    private final ApRecordStorage apStorage;

    private final ParPartyStorage partyStorage;

    private long currentMemoryScore;

    public StorageManager(long memoryScoreLimit, Session session, ImportInitHelper initHelper) {
        this.memoryScoreLimit = memoryScoreLimit;
        this.session = session;
        this.apStorage = new ApRecordStorage(this, LocalDateTime.now(), session, initHelper);
        this.partyStorage = new ParPartyStorage(session, this, initHelper);
    }

    public Session getSession() {
        return session;
    }

    @Override
    public long getAvailableMemoryScore() {
        long ams = memoryScoreLimit - currentMemoryScore;
        // always must be greater than zero
        Validate.isTrue(ams > 0);
        return ams;
    }

    @Override
    public void flushAndClear(boolean clearAll) {
        logger.debug("Clearing entities from persistent context, count: {}", persistEntities.size());

        session.flush();
        if (clearAll) {
            session.clear();
        } else {
            for (EntityWrapper ew : persistEntities) {
                //logger.debug("Evicting wrapper, class = {}", ew.getClass());

                ew.evictFrom(session);
            }
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
        // check memory limit
        currentMemoryScore += memoryScore;
        if (currentMemoryScore <= memoryScoreLimit) {
            return;
        }
        // flush & clear when overflowed the limit
        flushAndClear(false);
    }

    public void saveAccessPoints(Collection<AccessPointWrapper> items) {
        if (items.isEmpty()) {
            return;
        }
        apStorage.save(items);
        session.flush();
    }

    public void saveParties(Collection<PartyWrapper> items) {
        if (items.isEmpty()) {
            return;
        }
        partyStorage.save(items);
        session.flush();
    }

    /**
     * Stores all items and flushes persistent context.
     */
    public <T extends EntityWrapper> void saveGeneric(Collection<T> items) {
        if (items.isEmpty()) {
            return;
        }
        EntityStorage<T> storage = new EntityStorage<>(session, this);
        storage.save(items);
        session.flush();
    }
}
