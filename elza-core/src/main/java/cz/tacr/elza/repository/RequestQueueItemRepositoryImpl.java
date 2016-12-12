package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrRequestQueueItem;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Component
public class RequestQueueItemRepositoryImpl implements RequestQueueItemRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ArrRequestQueueItem findNext() {
        Query query = entityManager.createQuery("SELECT i FROM arr_request_queue_item i JOIN i.createChange c WHERE i.send = false ORDER BY c.changeDate ASC");
        query.setMaxResults(1);

        try {
            return (ArrRequestQueueItem) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
