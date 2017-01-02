package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class DigitizationRequestNodeRepositoryImpl implements DigitizationRequestNodeRepositoryCustom {

    private Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private EntityManager entityManager;

    @Override
    public Map<ArrDigitizationRequest, Integer> countByRequests(final Collection<ArrDigitizationRequest> requests) {

        if (CollectionUtils.isEmpty(requests)) {
            return new HashMap<>();
        }

        Query query = entityManager.createQuery("SELECT dr.requestId, COUNT(dr) FROM arr_digitization_request_node drn JOIN drn.digitizationRequest dr WHERE drn.digitizationRequest IN (:requests) GROUP BY dr.requestId");
        query.setParameter("requests", requests);

        Map<Integer, Integer> resultCount = new HashMap<>();
        Map<ArrDigitizationRequest, Integer> result = new HashMap<>();
        for (Object[] o : (List<Object[]>) query.getResultList()) {
            resultCount.put((Integer) o[0], ((Number) o[1]).intValue());
        }

        for (ArrDigitizationRequest request : requests) {
            Integer count = resultCount.get(request.getRequestId());
            result.put(request, count == null ? 0 : count);
        }

        return result;
    }

}
