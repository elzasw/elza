package cz.tacr.elza.deimport.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.Session;

/**
 * Stores entity wrappers in batches.
 */
class EntityStorage<T extends EntityWrapper> {

    protected final Session session;

    private final StorageListener storageListener;

    public EntityStorage(Session session, StorageListener storageListener) {
        this.session = Objects.requireNonNull(session);
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
            if (item.isCreate()) {
                creates.add(item);
            } else if (item.isUpdate()) {
                updates.add(item);
            }
            // NOP: ignored entity
        }
        // add to persistent context
        if (creates.size() > 0) {
            create(creates);
        }
        if (updates.size() > 0) {
            update(updates);
        }
        // notify wrappers
        creates.forEach(T::afterEntityPersist);
        updates.forEach(T::afterEntityPersist);
    }

    protected void create(List<T> items) {
        for (T item : items) {
            item.beforeEntityPersist(session);
            create(item, session);
        }
    }

    protected void update(Collection<T> items) {
        for (T item : items) {
            item.beforeEntityPersist(session);
            update(item, session);
        }
    }

    protected void create(T item, Session session) {
        Object entity = item.getEntity();
        session.persist(entity);
        fireEntityPersist(item, entity);
    }

    protected void update(T item, Session session) {
        Object entity = item.getEntity();
        entity = session.merge(entity);
        fireEntityPersist(item, entity);
    }

    private void fireEntityPersist(T item, Object entity) {
        if (storageListener != null) {
            storageListener.onEntityPersist(item, entity);
        }
    }
}
