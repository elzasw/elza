package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.common.db.HibernateUtils;

public class EntityStorage<T extends EntityWrapper> {

    protected final Session session;

    protected final StoredEntityCallback storedEntityCallback;

    public EntityStorage(Session session, StoredEntityCallback storedEntityCallback) {
        this.session = session;
        this.storedEntityCallback = storedEntityCallback;
    }

    public void store(Collection<T> ews) {
        List<T> persists = new ArrayList<>();
        List<T> merges = new ArrayList<>();
        for (T ew : ews) {
            switch (ew.getSaveMethod()) {
            case IGNORE:
                break;
            case CREATE:
                persists.add(ew);
                break;
            case UPDATE:
                merges.add(ew);
                break;
            }
        }
        persistEntities(persists);
        mergeEntities(merges);
        // flush all changes
        session.flush();
    }

    protected void persistEntity(T ew) {
        ew.beforeEntitySave(session);
        ew.persist(session);
        ew.afterEntitySave(session);
        storedEntityCallback.onStoredEntity(ew);
    }

    protected void mergeEntity(T ew) {
        Validate.isTrue(HibernateUtils.isInitialized(ew.getEntity()));

        ew.beforeEntitySave(session);
        // note: returned entity is in most cases same as input
        // !!! do not store return value of merge, we have to use same 
        // entity when calling evict as firstly obtained reference !!!
        ew.merge(session);
        ew.afterEntitySave(session);
        storedEntityCallback.onStoredEntity(ew);
    }

    protected void persistEntities(Collection<T> ews) {
        for (T ew : ews) {
            persistEntity(ew);
        }
    }

    protected void mergeEntities(Collection<T> ews) {
        for (T ew : ews) {
            mergeEntity(ew);
        }
    }
}
