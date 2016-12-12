package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRequest;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Component
public class RequestRepositoryImpl implements RequestRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ArrRequest> findRequests(final ArrFund fund, final ArrRequest.State state, final ArrRequest.ClassType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrRequest> q = cb.createQuery(ArrRequest.class);
        Root<ArrRequest> c = q.from(ArrRequest.class);


        List<Predicate> predicates = new ArrayList<>();

        if (state != null) {
            Predicate statePredicate = cb.equal(c.get("state"), state);
            predicates.add(statePredicate);
        }

        if (fund != null) {
            Predicate fundPredicate = cb.equal(c.get("fund"), fund);
            predicates.add(fundPredicate);
        }

        if (type != null) {
            Predicate typePredicate = cb.equal(c.get("discriminator"), type);
            predicates.add(typePredicate);
        }

        q.select(c).where(predicates.toArray(new Predicate[predicates.size()])).orderBy(cb.asc(c.get("createChange")));
        TypedQuery<ArrRequest> query = entityManager.createQuery(q);

        return query.getResultList();
    }

    @Override
    public boolean setState(final ArrRequest request, final ArrRequest.State oldState, final ArrRequest.State newState) {
        Query query = entityManager.createQuery("UPDATE arr_request r SET r.state = :newState WHERE r.state = :oldState AND r = :request");
        query.setParameter("newState", newState);
        query.setParameter("oldState", oldState);
        query.setParameter("request", request);
        return query.executeUpdate() > 0;
    }
}
