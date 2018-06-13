package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

/**
 * Stores entity wrappers in batches.
 */
class EntityStorage<T extends EntityWrapper> {

    protected final Session session;

    protected final StorageListener storageListener;

    public EntityStorage(Session session, StorageListener storageListener) {
        this.session = Validate.notNull(session);
        this.storageListener = storageListener;
    }

    /**
     * Store batch to persistent context.
     */
    public void save(Collection<T> items) {
        if (items.isEmpty()) {
            return;
        }
        List<T> creates = new ArrayList<>(items.size());
        List<T> updates = new ArrayList<>(items.size());

        for (T item : items) {
            switch (item.getPersistType()) {
            case CREATE:
                creates.add(item);
                break;
            case UPDATE:
                updates.add(item);
                break;
            default:
                // ignored entity
            }
        }
        // add to persistent context
        if (creates.size() > 0) {
            create(creates);
        }
        if (updates.size() > 0) {
            update(updates);
        }
    }

    protected void create(List<T> items) {
        for (T item : items) {
            // notify wrapper before save
            item.beforeEntityPersist(session);
            // persist item
            create(item);
            // notify wrapper after save
            item.afterEntityPersist();
        }
    }

    protected void update(Collection<T> items) {
        for (T item : items) {
            // notify wrapper before save
            item.beforeEntityPersist(session);
            // merge item
            update(item);
            // notify wrapper after save
            item.afterEntityPersist();
        }
    }

    protected void create(T item) {
        Object entity = item.getEntity();
        session.persist(entity);
        onEntityPersist(item, entity);
    }

    protected void update(T item) {
        Object entity = item.getEntity();
        entity = session.merge(entity);
        onEntityPersist(item, entity);
    }

    protected void onEntityPersist(T item, Object entity) {
        if (storageListener != null) {
            storageListener.onEntityPersist(item, entity);
        }
    }
}
