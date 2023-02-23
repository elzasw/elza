package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Implementace {@link BulkActionRunRepositoryCustom}
 */
public class BulkActionRunRepositoryImpl implements BulkActionRunRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ArrBulkActionRun> findBulkActionsByNodes(int fundVersionId,
                                                         Collection<Integer> nodeIds,
                                                         ArrBulkActionRun.State state) {
        Validate.notEmpty(nodeIds);

        String sql1 = "SELECT ar.bulk_action_code, MAX(ar.change_id) AS change_id FROM arr_bulk_action_run ar JOIN "
                + "(SELECT no.bulk_action_run_id FROM arr_bulk_action_node no WHERE no.node_id IN (:nodeIds) GROUP BY no.bulk_action_run_id HAVING count(*) = :count) x ON ar.bulk_action_run_id = x.bulk_action_run_id JOIN "
                + "(SELECT no.bulk_action_run_id FROM arr_bulk_action_node no GROUP BY no.bulk_action_run_id HAVING count(*) = :count) y ON ar.bulk_action_run_id = y.bulk_action_run_id ";

        if (state != null) {
            sql1 += "WHERE ar.state = :state ";
        }
        sql1 += "GROUP BY ar.bulk_action_code";

        String sql2 = "SELECT k.* FROM arr_bulk_action_run k JOIN (" + sql1 + ") l "
                + "ON k.bulk_action_code = l.bulk_action_code AND k.change_id = l.change_id WHERE k.fund_version_id = :fundVersionId";

        Session session = em.unwrap(Session.class);

        NativeQuery<ArrBulkActionRun> query = session.createNativeQuery(sql2, ArrBulkActionRun.class);

        query.setParameter("count", nodeIds.size());
        query.setParameterList("nodeIds", nodeIds);
        query.setParameter("fundVersionId", fundVersionId);
        if (state != null) {
            query.setParameter("state", state.name());
        }

        return query.getResultList();
    }

}
