package cz.tacr.elza.dataexchange.input.storage;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.ApChangeHolder;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;

/**
 * Storage manager for all imported items. Must be initialized with active
 * session.
 */
public class StorageManager implements StoredEntityCallback {

    private final List<EntityWrapper> persistedWrappers = new LinkedList<>();

    private final long memoryScoreLimit;

    private final Session session;

    private final ApChangeHolder apChangeHolder;

    private final ImportInitHelper initHelper;

    private long currentMemoryScore;

    public StorageManager(long memoryScoreLimit, Session session, ApChangeHolder apChangeHolder,
            ImportInitHelper initHelper) {
        this.memoryScoreLimit = memoryScoreLimit;
        this.session = session;
        this.apChangeHolder = apChangeHolder;
        this.initHelper = initHelper;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public void onStoredEntity(EntityWrapper ew) {
        Validate.notNull(ew);

        // estimate memory score
        long memoryScore = ew.getMemoryScore();
        // if zero skip this wrapper
        if (memoryScore <= 0) {
            return;
        }
        persistedWrappers.add(ew);
        // check memory limit
        currentMemoryScore += memoryScore;
        if (currentMemoryScore <= memoryScoreLimit) {
            return;
        }
		// flush & clear when overflowed the limit
        session.flush();
        clear();
    }

    public void storeRefUpdates(Collection<? extends RefUpdateWrapper> ruws) {
        if (ruws.isEmpty()) {
            return;
        }
        for (RefUpdateWrapper ruw : ruws) {
            if (ruw.isIgnored()) {
                continue;
            }
            if (ruw.isLoaded(session)) {
                ruw.merge(session);
                continue;
            }
            ruw.executeUpdateQuery(session);
        }
        // flush all changes
        session.flush();
    }

    public <T extends EntityWrapper> void storeGeneric(Collection<T> ews) {
        if (ews.isEmpty()) {
            return;
        }
        EntityStorage<T> storage = new EntityStorage<>(session, this);
        storage.store(ews);
    }

    public void storeAccessPoints(Collection<AccessPointWrapper> apws) {
        if (apws.isEmpty()) {
            return;
        }
        ApAccessPointStorage storage = new ApAccessPointStorage(session, this, apChangeHolder, initHelper);
        storage.store(apws);
    }

    public void storeParts(Collection<PartWrapper> pws) {
        if(pws.isEmpty()) {
            return;
        }
        ApPartStorage storage = new ApPartStorage(session, this, initHelper);
        storage.store(pws);
        initHelper.getAccessPointService().updatePartValues(pws);
    }

    public void storeItems(Collection<ItemWrapper> iws) {
        if(iws.isEmpty()) {
            return;
        }
        ApItemStorage storage = new ApItemStorage(session, this, initHelper);
        storage.store(iws);
    }

	public void clear() {
        for (EntityWrapper pw : persistedWrappers) {
            //logger.debug("Evicting wrapper, class = {}", ew.getClass());
            pw.evictFrom(session);
        }
        persistedWrappers.clear();
        currentMemoryScore = 0;
	}
}
