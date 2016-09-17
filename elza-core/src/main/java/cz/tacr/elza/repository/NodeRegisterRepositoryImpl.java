package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.utils.ObjectListIterator;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Implementace {@link NodeRegisterRepositoryCustom}.
 *
 * @author Martin Šlapa
 * @since 30.08.2016
 */
@Component
public class NodeRegisterRepositoryImpl implements NodeRegisterRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<Integer, List<RegRecord>> findByNodes(final Collection<Integer> nodeIds) {
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        Map<Integer, List<RegRecord>> result = new HashMap<>();
        while (iterator.hasNext()) {
            List<Integer> subNodeIds = iterator.next();

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<ArrNodeRegister> query = builder.createQuery(ArrNodeRegister.class);
            Root<ArrNodeRegister> root = query.from(ArrNodeRegister.class);

            root.fetch(ArrNodeRegister.RECORD, JoinType.INNER);

            Predicate predicateNodeIds = root.get(ArrNodeRegister.NODE_ID).in(subNodeIds);
            Predicate predicateDeleteChange = root.get(ArrNodeRegister.DELETE_CHANGE).isNull();
            query.where(predicateNodeIds, predicateDeleteChange);

            List<ArrNodeRegister> resultList = entityManager.createQuery(query).getResultList();

            for (ArrNodeRegister nodeRegister : resultList) {
                Integer nodeId = nodeRegister.getNodeId();
                List<RegRecord> records = result.get(nodeId);
                if (records == null) {
                    records = new ArrayList<>();
                    result.put(nodeId, records);
                }
                records.add(nodeRegister.getRecord());
            }

        }
        return result;
    }
}
