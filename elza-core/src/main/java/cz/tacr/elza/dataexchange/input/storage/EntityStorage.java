package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
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
        List<T> merges = new ArrayList<>();
        for (T ew : ews) {
            switch (ew.getSaveMethod()) {
            case IGNORE:
                continue;
            case CREATE:
                persistEntity(ew);
                continue;
            case UPDATE:
                merges.add(ew);
            }
        }
        mergeEntities(merges);
        // flush all changes
        session.flush();
    }

    protected void persistEntity(T ew) {
        ew.beforeEntitySave(session);
        session.persist(ew.getEntity());
        ew.afterEntitySave();
        storedEntityCallback.onStoredEntity(ew);
    }

    protected void mergeEntity(T ew) {
        Object entity = ew.getEntity();
        Validate.isTrue(HibernateUtils.isInitialized(entity));

        ew.beforeEntitySave(session);
        // note: returned entity is in most cases same as input
        // !!! do not store return value of merge, we have to use same 
        // entity when calling evict as firstly obtained reference !!! 
        session.merge(entity);
        ew.afterEntitySave();
        storedEntityCallback.onStoredEntity(ew);
    }

    protected void mergeEntities(Collection<T> ews) {
        for (T ew : ews) {
            mergeEntity(ew);
        }
    }
}
