package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Implementace {@link BulkActionRunRepositoryCustom}
 */
public class BulkActionRunRepositoryImpl implements BulkActionRunRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BulkActionRunRepository bulkActionRunRepository;

    @Override
    public List<ArrBulkActionRun> findBulkActionsByNodes(int fundVersionId,
                                                         List<Integer> nodeIds,
                                                         ArrBulkActionRun.State... states) {

        String sql = "SELECT k.bulk_action_run_id FROM arr_bulk_action_run k JOIN " +
                "(SELECT ar.bulk_action_code, MAX(ar.change_id) AS change_id FROM arr_bulk_action_run ar JOIN " +
                "(SELECT no.bulk_action_run_id FROM arr_bulk_action_node no WHERE no.node_id IN (:nodeIds) GROUP BY no.bulk_action_run_id HAVING count(*) = :count) x ON ar.bulk_action_run_id = x.bulk_action_run_id JOIN " +
                "(SELECT DISTINCT no.bulk_action_run_id FROM arr_bulk_action_node no GROUP BY no.bulk_action_run_id HAVING count(*) = :count) y ON ar.bulk_action_run_id = y.bulk_action_run_id " +
                "WHERE ar.state IN (:states) GROUP BY ar.bulk_action_code) l ON k.bulk_action_code = l.bulk_action_code AND k.change_id = l.change_id WHERE k.fund_version_id = :fundVersionId";

        List<String> stateNames = new ArrayList<>(states.length);
        for (ArrBulkActionRun.State state : states) {
            stateNames.add(state.name());
        }

        javax.persistence.Query query = entityManager.createNativeQuery(sql);
        query.setParameter("count", nodeIds.size());
        query.setParameter("nodeIds", nodeIds);
        query.setParameter("fundVersionId", fundVersionId);
        query.setParameter("states", stateNames);

        return bulkActionRunRepository.findAll(query.getResultList());
    }

}
