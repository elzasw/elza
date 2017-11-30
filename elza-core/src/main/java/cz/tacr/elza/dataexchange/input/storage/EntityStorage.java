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

    private final StorageListener storageListener;

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
            switch (item.getPersistMethod()) {
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

            create(item, session);
        }
    }

    protected void update(Collection<T> items) {
        for (T item : items) {
            // notify wrapper before save
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
        // notify wrapper after save
        item.afterEntityPersist();

        if (storageListener != null) {
            storageListener.onEntityPersist(item, entity);
        }
    }
}
