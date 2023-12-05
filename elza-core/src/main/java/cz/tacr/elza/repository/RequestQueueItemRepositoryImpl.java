package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrRequestQueueItem;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Component
public class RequestQueueItemRepositoryImpl implements RequestQueueItemRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ArrRequestQueueItem findNext(final Integer externalSystemId) {
        ArrRequestQueueItem result;

        Query query = entityManager.createQuery("SELECT i FROM arr_request_queue_item i JOIN i.createChange c " +
                " JOIN i.request r" +
                " WHERE i.send = false AND r.id IN (SELECT dar.id FROM arr_dao_request dar JOIN dar.digitalRepository dr WHERE dr.externalSystemId = :externalSystemId) ORDER BY c.changeDate ASC");
        query.setMaxResults(1);
        query.setParameter("externalSystemId", externalSystemId);

        try {
            result = (ArrRequestQueueItem) query.getSingleResult();
        } catch (NoResultException e) {
            result = null;
        }

        query = entityManager.createQuery("SELECT i FROM arr_request_queue_item i JOIN i.createChange c " +
                " JOIN i.request r" +
                " WHERE i.send = false AND r.id IN (SELECT dalr.id FROM arr_dao_link_request dalr JOIN dalr.digitalRepository dr WHERE dr.externalSystemId = :externalSystemId) ORDER BY c.changeDate ASC");
        query.setMaxResults(1);
        query.setParameter("externalSystemId", externalSystemId);

        try {
            final ArrRequestQueueItem singleResult = (ArrRequestQueueItem) query.getSingleResult();
            result = getOlderTime(result, singleResult);
        } catch (NoResultException e) {
            // Neexistuje výsledek
        }

        query = entityManager.createQuery("SELECT i FROM arr_request_queue_item i JOIN i.createChange c " +
                " JOIN i.request r" +
                " WHERE i.send = false AND r.id IN (SELECT dir.id FROM arr_digitization_request dir JOIN dir.digitizationFrontdesk df WHERE df.externalSystemId = :externalSystemId) ORDER BY c.changeDate ASC");
        query.setMaxResults(1);
        query.setParameter("externalSystemId", externalSystemId);

        try {
            final ArrRequestQueueItem singleResult = (ArrRequestQueueItem) query.getSingleResult();
            result = getOlderTime(result, singleResult);
        } catch (NoResultException e) {
            // Neexistuje výsledek
        }

        return result;
    }

    private ArrRequestQueueItem getOlderTime(ArrRequestQueueItem first, ArrRequestQueueItem second) {
        if (first == null && second == null) {
            return null;
        }

        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        final int result = first.getCreateChange().getChangeDate().compareTo(second.getCreateChange().getChangeDate());
        if (result <= 0) {
            return first;
        } else {
            return second;
        }

    }
}
