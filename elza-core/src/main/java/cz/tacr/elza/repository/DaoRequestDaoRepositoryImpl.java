package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class DaoRequestDaoRepositoryImpl implements DaoRequestDaoRepositoryCustom {

    private static Logger logger = LoggerFactory.getLogger(DaoRequestDaoRepositoryImpl.class);

    @Autowired
    private EntityManager entityManager;

    @Override
    public Map<ArrDaoRequest, Integer> countByRequests(final Collection<ArrDaoRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return new HashMap<>();
        }

        Query query = entityManager.createQuery("SELECT dr.requestId, COUNT(dr) FROM arr_dao_request_dao drn JOIN drn.daoRequest dr WHERE drn.daoRequest IN (:requests) GROUP BY dr.requestId");
        query.setParameter("requests", requests);

        Map<Integer, Integer> resultCount = new HashMap<>();
        Map<ArrDaoRequest, Integer> result = new HashMap<>();
        for (Object[] o : (List<Object[]>) query.getResultList()) {
            resultCount.put((Integer) o[0], ((Number) o[1]).intValue());
        }

        for (ArrDaoRequest request : requests) {
            Integer count = resultCount.get(request.getRequestId());
            result.put(request, count == null ? 0 : count);
        }

        return result;
    }
}
