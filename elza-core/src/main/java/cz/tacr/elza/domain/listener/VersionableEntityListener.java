package cz.tacr.elza.domain.listener;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.AbstractVersionableEntity;

/**
 * Listener pro kontrolu entit pře uložením.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 1. 2016
 */
@Component
public class VersionableEntityListener implements PreUpdateEventListener {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    private void init() {
        HibernateEntityManagerFactory hibernateEntityManagerFactory = (HibernateEntityManagerFactory) this.entityManagerFactory;
        SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) hibernateEntityManagerFactory.getSessionFactory();
        EventListenerRegistry registry = sessionFactoryImpl.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.PRE_UPDATE, this);
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
