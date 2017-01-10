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
    public ArrRequestQueueItem findNext(final Integer externalSystemId) {
        Query query = entityManager.createQuery("SELECT i FROM arr_request_queue_item i JOIN i.createChange c " +
                " LEFT JOIN i.request r LEFT JOIN r.digitalRepository dr " +
                " LEFT JOIN i.request r LEFT JOIN r.digitizationFrontdesk df " +
                " WHERE i.send = false AND ((dr IS NOT NULL AND dr.externalSystemId = :externalSystemId) OR (df IS NOT NULL AND df.externalSystemId = :externalSystemId)) ORDER BY c.changeDate ASC");
        query.setMaxResults(1);
        query.setParameter("externalSystemId", externalSystemId);

        try {
            return (ArrRequestQueueItem) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
