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
        fireEntityPersist(item);
    }

    protected void update(T item, Session session) {
        Object entity = item.getEntity();

        // note: returned entity is in most cases
        //       same as input
        // !!! do not store resultEntity back into wrapper
        //     wrapper have to use same entity when calling evict 
        //     as firstly obtained reference !!!                
        Object resultEntity = session.merge(entity);
        
        /*System.out.println("updateEntity input = " + entity.getClass() + " (" + System.identityHashCode(entity)
                + "), output = " + resultEntity.getClass() + " (" + System.identityHashCode(resultEntity) + ")");
                */
        fireEntityPersist(item);
    }

    private void fireEntityPersist(T item) {
        // notify wrapper after save
        item.afterEntityPersist();

        if (storageListener != null) {
            storageListener.onEntityPersist(item);
        }
    }
}
