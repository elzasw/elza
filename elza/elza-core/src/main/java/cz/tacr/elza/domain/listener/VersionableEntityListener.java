package cz.tacr.elza.domain.listener;

import org.hibernate.SessionFactory;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.AbstractVersionableEntity;
import jakarta.annotation.PostConstruct;

/**
 * Listener pro kontrolu entit pře uložením.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 1. 2016
 */
@Component
public class VersionableEntityListener implements PreUpdateEventListener {

    @Autowired
    private SessionFactory sessionFactory;

    @PostConstruct
    private void init() {
//        EventListenerRegistry registry = sessionFactory.getSessionFactoryOptions().getServiceRegistry().getService(EventListenerRegistry.class); //TODO hibernate search 6
//        registry.appendListeners(EventType.PRE_UPDATE, this);
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        Object entity = event.getEntity();

        if (entity instanceof AbstractVersionableEntity) {
            int versionPropertyIndex = event.getPersister().getEntityMetamodel().getVersionPropertyIndex();
            AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;

            Object[] oldState = event.getOldState();
            Integer oldStateVersion = (Integer) oldState[versionPropertyIndex];
            Integer entityVersion = versionable.getVersion();
            if (entityVersion < oldStateVersion) {
                throw new OptimisticLockingFailureException("V databázi existuje novější verze entity " + entity.toString()
                        + " než kterou se pokoušíte uložit. Verze entity v db " + oldStateVersion + ", ukládaná verze "
                        + entityVersion + ".");
            }
        }

        return false;
    }
}
