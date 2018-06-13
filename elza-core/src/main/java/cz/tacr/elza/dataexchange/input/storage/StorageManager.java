package cz.tacr.elza.dataexchange.input.storage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyWrapper;

/**
 * Storage manager for all imported items. Must be initialized with active
 * session.
 */
public class StorageManager implements StorageListener {

    private final List<Object> persistEntities = new LinkedList<>();

    private final long memoryScoreLimit;

    private final Session session;

    private final ApRecordStorage apStorage;

    private final ParPartyStorage partyStorage;

    private long currentMemoryScore;

    public StorageManager(long memoryScoreLimit, Session session, ImportInitHelper initHelper) {
        this.memoryScoreLimit = memoryScoreLimit;
        this.session = session;
        this.apStorage = new ApRecordStorage(this, LocalDateTime.now(), session, initHelper);
        this.partyStorage = new ParPartyStorage(this, session, initHelper);
    }

    public Session getSession() {
        return session;
    }

    public long getAvailableMemoryScore() {
        return memoryScoreLimit - currentMemoryScore;
    }

    /**
     * Flush all changes and completely clear the session. All existing references
     * will be detached.
     * 
     * @param clearAll
     *            If false only entities manages by storage will be cleared.
     */
    public void flushAndClear(boolean clearAll) {
        session.flush();
        if (clearAll) {
            session.clear();
        } else {
            persistEntities.forEach(session::evict);
        }
        persistEntities.clear();
        currentMemoryScore = 0;
    }

    @Override
    public void onEntityPersist(EntityWrapper item, Object entity) {
        Validate.notNull(item);
        Validate.notNull(entity);

        persistEntities.add(entity);
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
