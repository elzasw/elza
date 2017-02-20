package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class RequestRepositoryImpl implements RequestRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ArrRequest> findRequests(final ArrFund fund, final ArrRequest.State state, final ArrRequest.ClassType type,
                                         final String description, final LocalDateTime fromDate, final LocalDateTime toDate,
                                         final String subType) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrRequest> q = cb.createQuery(ArrRequest.class);
        Root<ArrRequest> c = q.from(ArrRequest.class);
        Path<ArrChange> ch = c.get("createChange");

        List<Predicate> predicates = new ArrayList<>();

        if (state != null) {
            Predicate statePredicate = cb.equal(c.get("state"), state);
            predicates.add(statePredicate);
        }

        if (fund != null) {
            Predicate fundPredicate = cb.equal(c.get("fund"), fund);
            predicates.add(fundPredicate);
        }

        if (type == null) {
            Predicate typePredicate = c.get("discriminator").in(ArrRequest.ClassType.DAO, ArrRequest.ClassType.DIGITIZATION);
            predicates.add(typePredicate);
        } else {
            Predicate typePredicate = cb.equal(c.get("discriminator"), type);
            predicates.add(typePredicate);
        }

        if (fromDate != null || toDate != null) {
            if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
                throw new BusinessException("Neplatný požadavek", ArrangementCode.REQUEST_INVALID).set("toDate", toDate).set("fromDate", fromDate);
            }

            Predicate fundPredicate = cb.between(ch.get("changeDate"), fromDate, toDate);
            predicates.add(fundPredicate);
        }

        q.select(c).where(predicates.toArray(new Predicate[predicates.size()])).orderBy(cb.desc(c.get("createChange")));
        TypedQuery<ArrRequest> query = entityManager.createQuery(q);

        ArrDaoRequest.Type daoType = null;
        if (subType != null) {
            daoType = ArrDaoRequest.Type.valueOf(subType);
        }

        List<ArrRequest> resultList = query.getResultList();
        if (StringUtils.hasText(description)) {
            Iterator<ArrRequest> iterator = resultList.iterator();
            while (iterator.hasNext()) {
                ArrRequest request = iterator.next();
                if (request.getDiscriminator() == ArrRequest.ClassType.DIGITIZATION) {
                    ArrDigitizationRequest digitizationRequest = (ArrDigitizationRequest) request;
                    if (digitizationRequest.getDescription() == null || !digitizationRequest.getDescription().matches(".*(" + description + ").*")) {
                        iterator.remove();
                    }
                } else if (request.getDiscriminator() == ArrRequest.ClassType.DAO) {
                    ArrDaoRequest daoRequest = (ArrDaoRequest) request;
                    if (daoRequest.getDescription() == null || !daoRequest.getDescription().matches(".*(" + description + ").*")) {
                        iterator.remove();
                    }
                } else {
                    // jiné požadavky nemají vůbec popis
                    iterator.remove();
                }
            }
        }

        if (daoType != null) {
            Iterator<ArrRequest> iterator = resultList.iterator();
            while (iterator.hasNext()) {
                ArrRequest request = iterator.next();
                if (request.getDiscriminator() == ArrRequest.ClassType.DAO) {
                    ArrDaoRequest daoRequest = (ArrDaoRequest) request;
                    if (!daoType.equals(daoRequest.getType())) {
                        iterator.remove();
                    }
                }
            }
        }

        return resultList;
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
